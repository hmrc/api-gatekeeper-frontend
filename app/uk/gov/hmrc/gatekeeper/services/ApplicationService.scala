/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.connectors._

import javax.inject.Inject
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models.GrantLength.GrantLength
import uk.gov.hmrc.gatekeeper.models.RateLimitTier.RateLimitTier
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.http.HeaderCarrier
import play.api.http.Status.NOT_FOUND
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.UpstreamErrorResponse

import java.time.LocalDateTime
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

class ApplicationService @Inject() (
    sandboxApplicationConnector: SandboxApplicationConnector,
    productionApplicationConnector: ProductionApplicationConnector,
    sandboxApiScopeConnector: SandboxApiScopeConnector,
    productionApiScopeConnector: ProductionApiScopeConnector,
    apmConnector: ApmConnector,
    developerConnector: DeveloperConnector,
    subscriptionFieldsService: SubscriptionFieldsService
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def resendVerification(application: Application, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    applicationConnectorFor(application).resendVerification(application.id, gatekeeperUserId)
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

    applicationConnectorFor(Some(PRODUCTION)).fetchAllApplicationsWithStateHistories().map(_.flatMap(appStateHistory => {
      buildChanges(appStateHistory.applicationId, appStateHistory.appName, appStateHistory.journeyVersion, appStateHistory.stateHistory)
    }))
  }

  def fetchApplications(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    val sandboxApplicationsFuture    = sandboxApplicationConnector.fetchAllApplications()
    val productionApplicationsFuture = productionApplicationConnector.fetchAllApplications()

    for {
      sandboxApps    <- sandboxApplicationsFuture
      productionApps <- productionApplicationsFuture
    } yield (sandboxApps ++ productionApps).distinct
  }

  def fetchApplications(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter)(implicit hc: HeaderCarrier): Future[List[ApplicationResponse]] = {
    val connectors: List[ApplicationConnector] = envFilter match {
      case ProductionEnvironment => List(productionApplicationConnector)
      case SandboxEnvironment    => List(sandboxApplicationConnector)
      case AnyEnvironment        => List(productionApplicationConnector, sandboxApplicationConnector)
    }

    apiFilter match {
      case OneOrMoreSubscriptions       =>
        val allFutures    = connectors.map(_.fetchAllApplications)
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

  def searchApplications(env: Option[Environment], params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedApplicationResponse] = {
    applicationConnectorFor(env).searchApplications(params)
  }

  def updateOverrides(application: ApplicationResponse, overrides: Set[OverrideFlag])(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    def findOverrideTypesWithInvalidScopes(overrides: Set[OverrideFlag], validScopes: Set[String]): Future[Set[OverrideFlag]] = {
      def containsInvalidScopes(validScopes: Set[String], scopes: Set[String]) = {
        !scopes.forall(validScopes)
      }

      def doesOverrideTypeContainInvalidScopes(overrideFlag: OverrideFlag): Boolean = overrideFlag match {
        case overrideFlagWithScopes: OverrideFlagWithScopes => containsInvalidScopes(validScopes, overrideFlagWithScopes.scopes)
        case _                                              => false
      }

      Future.successful(overrides.filter(doesOverrideTypeContainInvalidScopes))
    }

    application.access match {
      case _: Standard => {
        (
          for {
            validScopes                    <- apiScopeConnectorFor(application).fetchAll()
            overrideTypesWithInvalidScopes <- findOverrideTypesWithInvalidScopes(overrides, validScopes.map(_.key).toSet)
          } yield overrideTypesWithInvalidScopes
        ).flatMap(overrideTypes =>
          if (overrideTypes.nonEmpty) {
            Future.successful(UpdateOverridesFailureResult(overrideTypes))
          } else {
            applicationConnectorFor(application).updateOverrides(application.id, UpdateOverridesRequest(overrides))
          }
        )
      }
    }
  }

  def updateScopes(application: ApplicationResponse, scopes: Set[String])(implicit hc: HeaderCarrier): Future[UpdateScopesResult] = {

    application.access match {
      case _: AccessWithRestrictedScopes => {
        (
          for {
            validScopes     <- apiScopeConnectorFor(application).fetchAll()
            hasInvalidScopes = !scopes.subsetOf(validScopes.map(_.key).toSet)
          } yield hasInvalidScopes
        ).flatMap(hasInvalidScopes =>
          if (hasInvalidScopes) Future.successful(UpdateScopesInvalidScopesResult)
          else applicationConnectorFor(application).updateScopes(application.id, UpdateScopesRequest(scopes))
        )
      }
      case _: Standard                   => Future.successful(UpdateScopesInvalidScopesResult)
    }
  }

  def manageIpAllowlist(application: ApplicationResponse, required: Boolean, ipAllowlist: Set[String])(implicit hc: HeaderCarrier): Future[UpdateIpAllowlistResult] = {
    applicationConnectorFor(application).updateIpAllowlist(application.id, required, ipAllowlist)
  }

  def validateApplicationName(application: ApplicationResponse, name: String)(implicit hc: HeaderCarrier): Future[ValidateApplicationNameResult] = {
    applicationConnectorFor(application).validateApplicationName(application.id, name)
  }

  def updateApplicationName(application: ApplicationResponse, adminEmail: String, gatekeeperUser: String, name: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    if (application.name.equalsIgnoreCase(name)) {
      Future.successful(ApplicationUpdateSuccessResult)
    } else {
      application.collaborators.find(_.emailAddress == adminEmail).map(_.userId) match {
        case Some(instigator) => {
          val timestamp = LocalDateTime.now
          applicationConnectorFor(application).updateApplicationName(application.id, instigator, timestamp, gatekeeperUser, name)
        }
        case None             => Future.successful(ApplicationUpdateFailureResult)
      }
    }
  }

  def subscribeToApi(application: Application, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    apmConnector.subscribeToApi(application.id, apiIdentifier)
  }

  def unsubscribeFromApi(application: Application, context: ApiContext, version: ApiVersion)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnectorFor(application).unsubscribeFromApi(application.id, context, version)
  }

  def updateGrantLength(application: Application, grantLength: GrantLength)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnectorFor(application).updateGrantLength(application.id, grantLength)
  }

  def updateRateLimitTier(application: Application, tier: RateLimitTier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnectorFor(application).updateRateLimitTier(application.id, tier)
  }

  def deleteApplication(application: Application, gatekeeperUserId: String, requestByEmailAddress: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val reasons = "Application deleted by Gatekeeper user"
    val deleteRequest = DeleteApplicationByGatekeeper(gatekeeperUserId, requestByEmailAddress, reasons, LocalDateTime.now)
    applicationConnectorFor(application).deleteApplication(application.id, deleteRequest)
  }

  def blockApplication(application: Application, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApplicationBlockResult] = {
    applicationConnectorFor(application).blockApplication(application.id, BlockApplicationRequest(gatekeeperUserId))
  }

  def unblockApplication(application: Application, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApplicationUnblockResult] = {
    applicationConnectorFor(application).unblockApplication(application.id, UnblockApplicationRequest(gatekeeperUserId))
  }

  def approveUplift(application: Application, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[ApproveUpliftSuccessful] = {
    applicationConnectorFor(application).approveUplift(application.id, gatekeeperUserId)
  }

  def rejectUplift(application: Application, gatekeeperUserId: String, rejectionReason: String)(implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    applicationConnectorFor(application).rejectUplift(application.id, gatekeeperUserId, rejectionReason)
  }

  def createPrivOrROPCApp(
      appEnv: Environment,
      appName: String,
      appDescription: String,
      collaborators: List[Collaborator],
      access: AppAccess
    )(implicit hc: HeaderCarrier
    ): Future[CreatePrivOrROPCAppResult] = {
    val req = CreatePrivOrROPCAppRequest(appEnv.toString, appName, appDescription, collaborators, access)

    appEnv match {
      case PRODUCTION => productionApplicationConnector.createPrivOrROPCApp(req)
      case SANDBOX    => sandboxApplicationConnector.createPrivOrROPCApp(req)
    }
  }

  def addTeamMember(app: Application, teamMember: Collaborator)(implicit hc: HeaderCarrier): Future[Unit] = {
    for {
      response <- apmConnector.addTeamMember(app.id, AddTeamMemberRequest(teamMember.emailAddress, teamMember.role, None))
    } yield response

  }

  def removeTeamMember(app: Application, teamMemberToRemove: String, requestingEmail: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val applicationConnector = applicationConnectorFor(app)
    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set(teamMemberToRemove, requestingEmail))
      response      <- applicationConnector.removeCollaborator(app.id, teamMemberToRemove, requestingEmail, adminsToEmail)
    } yield response
  }

  private def getAdminsToEmail(collaborators: Set[Collaborator], excludes: Set[String])(implicit hc: HeaderCarrier): Future[Set[String]] = {
    val adminEmails = collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR).map(_.emailAddress).filterNot(excludes.contains(_))

    developerConnector.fetchByEmails(adminEmails)
      .map(registeredUsers =>
        registeredUsers.filter(_.verified)
          .map(_.email)
          .toSet
      )
  }

  def applicationConnectorFor(application: Application): ApplicationConnector =
    if (application.deployedTo == "PRODUCTION") productionApplicationConnector else sandboxApplicationConnector

  def applicationConnectorFor(environment: Option[Environment]): ApplicationConnector =
    if (environment == Some(PRODUCTION)) productionApplicationConnector else sandboxApplicationConnector

  def apiScopeConnectorFor(application: Application): ApiScopeConnector =
    if (application.deployedTo == "PRODUCTION") productionApiScopeConnector else sandboxApiScopeConnector

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)

  def doesApplicationHaveSubmissions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Boolean] = {
    productionApplicationConnector.doesApplicationHaveSubmissions(applicationId)
  }
}
