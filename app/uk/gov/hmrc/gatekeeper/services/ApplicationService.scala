/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gatekeeper.services

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, OverrideFlag}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{ApplicationNameValidationResult, CreateApplicationRequestV1, CreationAccess}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.{ApplicationLogger, ClockNow}
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector.AppWithSubscriptionsForCsvResponse
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models._

@Singleton
class ApplicationService @Inject() (
    sandboxApplicationConnector: SandboxApplicationConnector,
    productionApplicationConnector: ProductionApplicationConnector,
    sandboxApiScopeConnector: SandboxApiScopeConnector,
    productionApiScopeConnector: ProductionApiScopeConnector,
    apmConnector: ApmConnector,
    tpoConnector: ThirdPartyOrchestratorConnector,
    developerConnector: DeveloperConnector,
    subscriptionFieldsService: SubscriptionFieldsService,
    commandConnector: ApplicationCommandConnector,
    val clock: Clock
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def resendVerification(application: ApplicationWithCollaborators, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    commandConnector.dispatch(application.id, ApplicationCommands.ResendRequesterEmailVerification(gatekeeperUserId, instant()), Set.empty[LaxEmailAddress])
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def fetchStateHistory(applicationId: ApplicationId, environment: Environment)(implicit hc: HeaderCarrier): Future[List[StateHistory]] = {
    applicationConnectorFor(Some(environment)).fetchStateHistory(applicationId)
  }

  def fetchProdAppStateHistories()(implicit hc: HeaderCarrier): Future[List[ApplicationStateHistoryChange]] = {
    def buildChanges(appId: ApplicationId, appName: String, journeyVersion: Int, stateHistory: List[ApplicationStateHistoryItem]): List[ApplicationStateHistoryChange] = {
      stateHistory match {
        case state1 :: state2 :: others => ApplicationStateHistoryChange(
            appId.value.toString(),
            appName,
            journeyVersion.toString,
            state1.state.toString,
            state1.timestamp.toString,
            state2.state.toString,
            state2.timestamp.toString
          ) :: buildChanges(appId, appName, journeyVersion, state2 :: others)

        case finalState :: Nil => List(ApplicationStateHistoryChange(
            appId.value.toString(),
            appName,
            journeyVersion.toString,
            finalState.state.toString,
            finalState.timestamp.toString,
            "",
            ""
          ))

        case Nil => {
          logger.warn(s"Found a PRODUCTION application ${appId} without any state history while running the Application State History report")
          List()
        }
      }
    }

    applicationConnectorFor(Some(Environment.PRODUCTION)).fetchAllApplicationsWithStateHistories().map(_.flatMap(appStateHistory => {
      buildChanges(appStateHistory.applicationId, appStateHistory.appName, appStateHistory.journeyVersion, appStateHistory.stateHistory)
    }))
  }

  def fetchApplications(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    val sandboxApplicationsFuture    = sandboxApplicationConnector.fetchAllApplications()
    val productionApplicationsFuture = productionApplicationConnector.fetchAllApplications()

    for {
      sandboxApps    <- sandboxApplicationsFuture
      productionApps <- productionApplicationsFuture
    } yield (sandboxApps ++ productionApps).distinct
  }

  def fetchApplications(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter)(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    val connectors: List[ApplicationConnector] = envFilter match {
      case ProductionEnvironment => List(productionApplicationConnector)
      case SandboxEnvironment    => List(sandboxApplicationConnector)
      case AnyEnvironment        => List(productionApplicationConnector, sandboxApplicationConnector)
    }

    apiFilter match {
      case OneOrMoreSubscriptions       =>
        val allFutures    = connectors.map(_.fetchAllApplications())
        val noSubsFutures = connectors.map(_.fetchAllApplicationsWithNoSubscriptions())
        for {
          all    <- combine(allFutures)
          noSubs <- combine(noSubsFutures)
        } yield all.filterNot(app => noSubs.contains(app)).distinct
      case NoSubscriptions              =>
        val noSubsFutures = connectors.map(_.fetchAllApplicationsWithNoSubscriptions())
        for {
          apps <- combine(noSubsFutures)
        } yield apps.distinct
      case Value(subscribesTo, version) =>
        val futures = connectors.map(_.fetchAllApplicationsBySubscription(subscribesTo, version))
        for {
          apps <- combine(futures)
        } yield apps.distinct
      case _                            => fetchApplications
    }
  }

  def fetchApplication(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    productionApplicationConnector.fetchApplication(appId).recoverWith {
      case UpstreamErrorResponse(_, NOT_FOUND, _, _) => sandboxApplicationConnector.fetchApplication(appId)
    }
  }

  def searchApplications(env: Option[Environment], params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedApplications] = {
    applicationConnectorFor(env).searchApplications(params)
  }

  def fetchApplicationsWithSubscriptions(env: Option[Environment])(implicit hc: HeaderCarrier): Future[List[AppWithSubscriptionsForCsvResponse]] = {
    applicationConnectorFor(env).fetchApplicationsWithSubscriptions()
  }

  def updateOverrides(application: ApplicationWithCollaborators, overrides: Set[OverrideFlag], gatekeeperUserId: String)(implicit hc: HeaderCarrier)
      : Future[UpdateOverridesResult] = {
    def findOverrideTypesWithInvalidScopes(overrides: Set[OverrideFlag], validScopes: Set[String]): Future[Set[OverrideFlag]] = {
      def containsInvalidScopes(validScopes: Set[String], scopes: Set[String]) = {
        !scopes.forall(validScopes)
      }

      def doesOverrideTypeContainInvalidScopes(overrideFlag: OverrideFlag): Boolean = overrideFlag match {
        case OverrideFlag.SuppressIvForAgents(scopes)        => containsInvalidScopes(validScopes, scopes)
        case OverrideFlag.SuppressIvForOrganisations(scopes) => containsInvalidScopes(validScopes, scopes)
        case OverrideFlag.SuppressIvForIndividuals(scopes)   => containsInvalidScopes(validScopes, scopes)
        case OverrideFlag.GrantWithoutConsent(scopes)        => containsInvalidScopes(validScopes, scopes)
        case _                                               => false
      }

      Future.successful(overrides.filter(doesOverrideTypeContainInvalidScopes))
    }

    application.access match {
      case _: Access.Standard                    => {
        (
          for {
            validScopes                    <- apiScopeConnectorFor(application).fetchAll()
            overrideTypesWithInvalidScopes <- findOverrideTypesWithInvalidScopes(overrides, validScopes.map(_.key).toSet)
          } yield overrideTypesWithInvalidScopes
        ).flatMap(overrideTypes =>
          if (overrideTypes.nonEmpty) {
            Future.successful(UpdateOverridesFailureResult(overrideTypes))
          } else {
            val cmd = ApplicationCommands.ChangeApplicationAccessOverrides(gatekeeperUserId, overrides, instant())
            commandConnector.dispatch(application.id, cmd, Set.empty[LaxEmailAddress])
              .map(_.fold(_ => UpdateOverridesFailureResult(), _ => UpdateOverridesSuccessResult))
          }
        )
      }
      case _: Access.Privileged | _: Access.Ropc => Future.successful(UpdateOverridesFailureResult())
    }
  }

  def updateScopes(application: ApplicationWithCollaborators, scopes: Set[String], gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {

    application.access match {
      case _: Access.Privileged | _: Access.Ropc => {
        (
          for {
            validScopes     <- apiScopeConnectorFor(application).fetchAll()
            hasInvalidScopes = !scopes.subsetOf(validScopes.map(_.key).toSet)
          } yield hasInvalidScopes
        ).flatMap(hasInvalidScopes =>
          if (hasInvalidScopes) Future.successful(UpdateScopesInvalidScopesResult)
          else {
            val cmd = ApplicationCommands.ChangeApplicationScopes(gatekeeperUserId, scopes, instant())
            commandConnector.dispatch(application.id, cmd, Set.empty[LaxEmailAddress])
              .map(_.fold(_ => UpdateScopesInvalidScopesResult, _ => UpdateScopesSuccessResult))
          }
        )
      }
      case _: Access.Standard                    => Future.successful(UpdateScopesInvalidScopesResult)
    }
  }

  def manageIpAllowlist(
      application: ApplicationWithCollaborators,
      required: Boolean,
      ipAllowlist: Set[String],
      gatekeeperUser: String
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {
    val currentIpAllowlist = application.details.ipAllowlist.allowlist.map(CidrBlock(_)).toList
    val newIpAllowlist     = ipAllowlist.map(CidrBlock(_)).toList

    commandConnector.dispatch(
      application.id,
      ApplicationCommands.ChangeIpAllowlist(
        Actors.GatekeeperUser(gatekeeperUser),
        instant(),
        required,
        currentIpAllowlist,
        newIpAllowlist
      ),
      Set.empty[LaxEmailAddress]
    )
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def validateApplicationName(application: ApplicationWithCollaborators, name: String)(implicit hc: HeaderCarrier): Future[ApplicationNameValidationResult] = {
    tpoConnector.validateName(name, Some(application.id), application.deployedTo)
  }

  def validateNewApplicationName(environment: Environment, name: String)(implicit hc: HeaderCarrier): Future[ApplicationNameValidationResult] = {
    tpoConnector.validateName(name, None, environment)
  }

  def updateApplicationName(
      application: ApplicationWithCollaborators,
      adminEmail: LaxEmailAddress,
      gatekeeperUser: String,
      newName: ValidatedApplicationName
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {
    if (application.name.value.equalsIgnoreCase(newName.value)) {
      Future.successful(ApplicationUpdateSuccessResult)
    } else {
      application.collaborators.find(_.emailAddress == adminEmail).map(_.userId) match {
        case Some(instigator) =>
          commandConnector.dispatch(application.id, ApplicationCommands.ChangeProductionApplicationName(gatekeeperUser, instigator, instant(), newName), Set.empty[LaxEmailAddress])
            .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
        case None             => Future.successful(ApplicationUpdateFailureResult)
      }
    }
  }

  def updateGrantLength(application: ApplicationWithCollaborators, grantLength: GrantLength, gatekeeperUser: String)(implicit hc: HeaderCarrier)
      : Future[ApplicationUpdateResult] = {
    commandConnector.dispatch(application.id, ApplicationCommands.ChangeGrantLength(gatekeeperUser, instant(), grantLength), Set.empty[LaxEmailAddress])
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def updateDeleteRestriction(
      applicationId: ApplicationId,
      allowDelete: Boolean,
      gatekeeperUser: String,
      reason: String
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {

    val appCmdResult = allowDelete match {
      case true  => commandConnector.dispatch(applicationId, ApplicationCommands.AllowApplicationDelete(gatekeeperUser, reason, instant()), Set.empty[LaxEmailAddress])
      case false => commandConnector.dispatch(applicationId, ApplicationCommands.RestrictApplicationDelete(gatekeeperUser, reason, instant()), Set.empty[LaxEmailAddress])
    }

    appCmdResult.map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def updateRateLimitTier(application: ApplicationWithCollaborators, tier: RateLimitTier, gatekeeperUser: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    commandConnector.dispatch(application.id, ApplicationCommands.ChangeRateLimitTier(gatekeeperUser, instant(), tier), Set.empty[LaxEmailAddress])
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def deleteApplication(
      application: ApplicationWithCollaborators,
      gatekeeperUser: String,
      requestByEmailAddress: LaxEmailAddress
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {
    val reasons = "Application deleted by Gatekeeper user"
    commandConnector.dispatch(
      application.id,
      ApplicationCommands.DeleteApplicationByGatekeeper(gatekeeperUser, requestByEmailAddress, reasons, instant()),
      Set.empty[LaxEmailAddress]
    )
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def blockApplication(application: ApplicationWithCollaborators, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApplicationBlockResult] = {
    val cmd = ApplicationCommands.BlockApplication(gatekeeperUserId, instant())
    commandConnector.dispatch(application.id, cmd, Set.empty[LaxEmailAddress])
      .map(_.fold(_ => ApplicationBlockFailureResult, _ => ApplicationBlockSuccessResult))
  }

  def unblockApplication(application: ApplicationWithCollaborators, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApplicationUnblockResult] = {
    val cmd = ApplicationCommands.UnblockApplication(gatekeeperUserId, instant())
    commandConnector.dispatch(application.id, cmd, Set.empty[LaxEmailAddress])
      .map(_.fold(_ => ApplicationUnblockFailureResult, _ => ApplicationUnblockSuccessResult))
  }

  def createPrivApp(
      appEnv: Environment,
      appName: String,
      appDescription: String,
      collaborators: List[Collaborator]
    )(implicit hc: HeaderCarrier
    ): Future[CreatePrivAppResult] = {
    val req = CreateApplicationRequestV1(
      name = ApplicationName(appName),
      access = CreationAccess.Privileged,
      description = Some(appDescription).filterNot(_.isBlank()),
      environment = appEnv,
      collaborators.toSet,
      subscriptions = None
    )

    appEnv match {
      case Environment.PRODUCTION => productionApplicationConnector.createPrivApp(req)
      case Environment.SANDBOX    => sandboxApplicationConnector.createPrivApp(req)
    }
  }

  def applicationConnectorFor(application: ApplicationWithCollaborators): ApplicationConnector = applicationConnectorFor(Some(application.deployedTo))

  def applicationConnectorFor(environment: Option[Environment]): ApplicationConnector =
    if (environment.contains(Environment.PRODUCTION)) productionApplicationConnector else sandboxApplicationConnector

  def apiScopeConnectorFor(application: ApplicationWithCollaborators): ApiScopeConnector =
    if (application.isProduction) productionApiScopeConnector else sandboxApiScopeConnector

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)

  def doesApplicationHaveSubmissions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    productionApplicationConnector.doesApplicationHaveSubmissions(applicationId)
  }

  def doesApplicationHaveTermsOfUseInvitation(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    productionApplicationConnector.doesApplicationHaveTermsOfUseInvitation(applicationId)
  }
}
