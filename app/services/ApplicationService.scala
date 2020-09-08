/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors._
import javax.inject.Inject
import model.SubscriptionFields._
import model.Environment._
import model.RateLimitTier.RateLimitTier
import model._
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException}
import services.SubscriptionFieldsService.DefinitionsByApiVersion

import scala.concurrent.{ExecutionContext, Future}

class ApplicationService @Inject()(sandboxApplicationConnector: SandboxApplicationConnector,
                                   productionApplicationConnector: ProductionApplicationConnector,
                                   sandboxApiScopeConnector: SandboxApiScopeConnector,
                                   productionApiScopeConnector: ProductionApiScopeConnector,
                                   developerConnector: DeveloperConnector,
                                   subscriptionFieldsService: SubscriptionFieldsService)(implicit ec: ExecutionContext) {

  def resendVerification(application: Application, gatekeeperUserId: String)
                        (implicit hc: HeaderCarrier): Future[ResendVerificationSuccessful] = {
    applicationConnectorFor(application).resendVerification(application.id, gatekeeperUserId)
  }

  def fetchApplications(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    val sandboxApplicationsFuture = sandboxApplicationConnector.fetchAllApplications()
    val productionApplicationsFuture = productionApplicationConnector.fetchAllApplications()

    for {
      sandboxApps <- sandboxApplicationsFuture
      productionApps <- productionApplicationsFuture
    } yield (sandboxApps ++ productionApps).distinct
  }

  def fetchApplications(apiFilter: ApiFilter[String], envFilter: ApiSubscriptionInEnvironmentFilter)(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    val connectors: Seq[ApplicationConnector] = envFilter match {
      case ProductionEnvironment => Seq(productionApplicationConnector)
      case SandboxEnvironment => Seq(sandboxApplicationConnector)
      case AnyEnvironment => Seq(productionApplicationConnector, sandboxApplicationConnector)
    }

    apiFilter match {
      case OneOrMoreSubscriptions =>
        val allFutures = connectors.map(_.fetchAllApplications)
        val noSubsFutures = connectors.map(_.fetchAllApplicationsWithNoSubscriptions())
        for {
          all <- combine(allFutures)
          noSubs <- combine(noSubsFutures)
        } yield all.filterNot(app => noSubs.contains(app)).distinct
      case NoSubscriptions =>
        val noSubsFutures = connectors.map(_.fetchAllApplicationsWithNoSubscriptions())
        for {
          apps <- combine(noSubsFutures)
        } yield apps.distinct
      case Value(subscribesTo, version) =>
        val futures = connectors.map(_.fetchAllApplicationsBySubscription(subscribesTo, version))
        for {
          apps <- combine(futures)
        } yield apps.distinct
      case _ => fetchApplications
    }
  }

  def fetchApplicationsByEmail(email: String)(implicit hc: HeaderCarrier): Future[Seq[Application]] = {
    val sandboxApplicationsFuture = sandboxApplicationConnector.fetchApplicationsByEmail(email)
    val productionApplicationsFuture = productionApplicationConnector.fetchApplicationsByEmail(email)

    for {
      sandboxApps <- sandboxApplicationsFuture
      productionApps <- productionApplicationsFuture
    } yield (sandboxApps ++ productionApps).distinct
  }

  def fetchApplication(appId: ApplicationId)(implicit hc: HeaderCarrier): Future[ApplicationWithHistory] = {
    productionApplicationConnector.fetchApplication(appId).recoverWith {
      case _: NotFoundException => sandboxApplicationConnector.fetchApplication(appId)
    }
  }

  def searchApplications(env: Option[Environment], params: Map[String, String])(implicit hc: HeaderCarrier): Future[PaginatedSubscribedApplicationResponse] = {
    def addSubscriptionsToApplications(par: PaginatedApplicationResponse, subscriptions: Seq[SubscriptionResponse]) = {

      val apps = par.applications.map(ar => {
        val filteredSubs = subscriptions.filter(_.applications.exists(_ == ar.id.toString))
        .map(sub => SubscriptionNameAndVersion(sub.apiIdentifier.apiContext.value, sub.apiIdentifier.version)).sortBy(sub => sub.name)
        SubscribedApplicationResponse.createFrom(ar, filteredSubs)
      }).distinct

      PaginatedSubscribedApplicationResponse(par, apps)
    }

    val connector = applicationConnectorFor(env)

    val appsFuture = connector.searchApplications(params)

    //connector.fetchAllSubscriptions()
    val subsFuture = Future.successful(Seq.empty)

    for {
      par: PaginatedApplicationResponse <- appsFuture
      subs: Seq[SubscriptionResponse] <- subsFuture
    } yield addSubscriptionsToApplications(par, subs)
  }

  def fetchApplicationSubscriptions(application: Application)(implicit hc: HeaderCarrier): Future[Seq[Subscription]] = {
    def toApiSubscriptionStatuses(allDefinitionsByApiVersion: DefinitionsByApiVersion,
                                  subscription: SubscriptionWithoutFields,
                                  version: VersionSubscriptionWithoutFields): Future[VersionSubscription] = {

    val apiIdentifier = APIIdentifier(subscription.apiContext, version.version.version)

    subscriptionFieldsService
      .fetchFieldsWithPrefetchedDefinitions(application, apiIdentifier, allDefinitionsByApiVersion)
      .map {
        fields: Seq[SubscriptionFieldValue] =>
          VersionSubscription(
            version.version,
            version.subscribed,
            SubscriptionFieldsWrapper(application.id, application.clientId, subscription.apiContext, version.version.version, fields))
      }
    }

    def toApiVersions(allDefinitionsByApiVersion: DefinitionsByApiVersion, subscription: SubscriptionWithoutFields): Future[Subscription] = {
      val apiSubscriptionStatuses = subscription.versions
          .filterNot(_.version.status == APIStatus.RETIRED)
          .filterNot(s => s.version.status == APIStatus.DEPRECATED && !s.subscribed)
          .sortWith(APIDefinition.descendingVersion)
          .map(toApiSubscriptionStatuses(allDefinitionsByApiVersion, subscription, _))

      Future.sequence(apiSubscriptionStatuses)
        .map(versionSubscrioptions => {
            Subscription(subscription.name, subscription.serviceName, subscription.apiContext, versionSubscrioptions)
        })
    }

    for {
      allDefinitionsByApiVersion <- subscriptionFieldsService.fetchAllFieldDefinitions(application.deployedTo)
      subscriptions <- applicationConnectorFor(application).fetchApplicationSubscriptions(application.id)
      subscriptionsWithSubscriptionFields <- Future.sequence(subscriptions.map(subscription => toApiVersions(allDefinitionsByApiVersion, subscription)))
    } yield subscriptionsWithSubscriptionFields
  }

  def updateOverrides(application: ApplicationResponse, overrides: Set[OverrideFlag])(implicit hc: HeaderCarrier): Future[UpdateOverridesResult] = {
    def findOverrideTypesWithInvalidScopes(overrides: Set[OverrideFlag], validScopes: Set[String]): Future[Set[OverrideFlag]] = {
      def containsInvalidScopes(validScopes: Set[String], scopes: Set[String]) = {
        !scopes.forall(validScopes)
      }

      def doesOverrideTypeContainInvalidScopes(overrideFlag: OverrideFlag): Boolean = overrideFlag match {
        case overrideFlagWithScopes: OverrideFlagWithScopes => containsInvalidScopes(validScopes, overrideFlagWithScopes.scopes)
        case _ => false
      }

      Future.successful(overrides.filter(doesOverrideTypeContainInvalidScopes))
    }

    application.access match {
      case _: Standard => {
        (
          for {
            validScopes <- apiScopeConnectorFor(application).fetchAll()
            overrideTypesWithInvalidScopes <- findOverrideTypesWithInvalidScopes(overrides, validScopes.map(_.key).toSet)
          } yield overrideTypesWithInvalidScopes
        ).flatMap(overrideTypes =>
          if(overrideTypes.nonEmpty) {
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
            validScopes <- apiScopeConnectorFor(application).fetchAll()
            hasInvalidScopes = !scopes.subsetOf(validScopes.map(_.key).toSet)
          } yield hasInvalidScopes
        ).flatMap(hasInvalidScopes =>
          if (hasInvalidScopes) Future.successful(UpdateScopesInvalidScopesResult)
          else applicationConnectorFor(application).updateScopes(application.id, UpdateScopesRequest(scopes))
        )
      }
    }
  }

  def manageWhitelistedIp(application: ApplicationResponse, whitelistedIp: Set[String])(implicit hc: HeaderCarrier): Future[UpdateIpWhitelistResult] = {
    applicationConnectorFor(application).manageIpWhitelist(application.id, whitelistedIp)
  }

  def subscribeToApi(application: Application, context: ApiContext, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val applicationConnector: ApplicationConnector = applicationConnectorFor(application)

    val apiIdentifier = APIIdentifier(context, version)

    trait HasSucceeded
    object HasSucceeded extends HasSucceeded

    def ensureEmptyValuesWhenNoneExists(fieldDefinitions: Seq[SubscriptionFieldDefinition]): Future[HasSucceeded] = {
      for {
        oldValues <- subscriptionFieldsService.fetchFieldsValues(application, fieldDefinitions, apiIdentifier)
        saveResponse <- subscriptionFieldsService.saveBlankFieldValues(application, context, version, oldValues)
      } yield saveResponse match {
        case SaveSubscriptionFieldsSuccessResponse => HasSucceeded
        case error =>
          val errorMessage = s"Failed to save blank subscription field values: $error"
          throw new RuntimeException(errorMessage)
      }
    }

    def ensureSavedValuesForAnyDefinitions(defns: Seq[SubscriptionFieldDefinition]): Future[HasSucceeded] = {
      if (defns.nonEmpty){
        ensureEmptyValuesWhenNoneExists(defns)
      } else {
        Future.successful(HasSucceeded)
      }
    }

    val subscribeResponse = applicationConnector.subscribeToApi(application.id, apiIdentifier)
    val fieldDefinitions = subscriptionFieldsService.fetchFieldDefinitions(application.deployedTo, apiIdentifier)

    fieldDefinitions
      .flatMap(ensureSavedValuesForAnyDefinitions)
      .flatMap(_ => subscribeResponse)
  }

  def unsubscribeFromApi(application: Application, context: ApiContext, version: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
      applicationConnectorFor(application).unsubscribeFromApi(application.id, context, version)
  }

  def updateRateLimitTier(application: Application, tier: RateLimitTier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    applicationConnectorFor(application).updateRateLimitTier(application.id, tier)
  }

  def deleteApplication(application: Application, gatekeeperUserId: String, requestByEmailAddress: String)(implicit hc: HeaderCarrier): Future[ApplicationDeleteResult] = {
    applicationConnectorFor(application).deleteApplication(application.id, DeleteApplicationRequest(gatekeeperUserId, requestByEmailAddress))
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

  def rejectUplift(application: Application, gatekeeperUserId: String, rejectionReason: String)
                  (implicit hc: HeaderCarrier): Future[RejectUpliftSuccessful] = {
    applicationConnectorFor(application).rejectUplift(application.id, gatekeeperUserId, rejectionReason)
  }

  def createPrivOrROPCApp(appEnv: Environment, appName: String, appDescription: String, collaborators: Seq[Collaborator], access: AppAccess)(implicit hc: HeaderCarrier): Future[CreatePrivOrROPCAppResult] = {
    val req = CreatePrivOrROPCAppRequest(appEnv.toString, appName, appDescription, collaborators, access)

    appEnv match {
      case PRODUCTION => productionApplicationConnector.createPrivOrROPCApp(req)
      case SANDBOX => sandboxApplicationConnector.createPrivOrROPCApp(req)
    }
  }

  def addTeamMember(app: Application, teamMember: Collaborator, requestingEmail: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val applicationConnector = applicationConnectorFor(app)
    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set(requestingEmail))
      developer <- developerConnector.fetchByEmail(teamMember.emailAddress)
      response <- applicationConnector.addCollaborator(app.id, AddTeamMemberRequest(requestingEmail, teamMember, developer.verified.isDefined, adminsToEmail.toSet))
    } yield response

  }

  def removeTeamMember(app: Application, teamMemberToRemove: String, requestingEmail: String)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    val applicationConnector = applicationConnectorFor(app)
    for {
      adminsToEmail <- getAdminsToEmail(app.collaborators, excludes = Set(teamMemberToRemove, requestingEmail))
      response <- applicationConnector.removeCollaborator(app.id, teamMemberToRemove, requestingEmail, adminsToEmail)
    } yield response
  }

  private def getAdminsToEmail(collaborators: Set[Collaborator], excludes: Set[String])(implicit hc: HeaderCarrier): Future[Seq[String]] = {
    val adminEmails = collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR).map(_.emailAddress).filterNot(excludes.contains(_))

    developerConnector.fetchByEmails(adminEmails).map(_.filter(_.verified.contains(true)).map(_.email))
  }

  def applicationConnectorFor(application: Application): ApplicationConnector =
    if (application.deployedTo == "PRODUCTION") productionApplicationConnector else sandboxApplicationConnector

  def applicationConnectorFor(environment: Option[Environment]): ApplicationConnector =
    if (environment == Some(PRODUCTION)) productionApplicationConnector else sandboxApplicationConnector

  def apiScopeConnectorFor(application: Application): ApiScopeConnector =
    if (application.deployedTo == "PRODUCTION") productionApiScopeConnector else sandboxApiScopeConnector

  private def combine[T](futures: Seq[Future[Seq[T]]]): Future[Seq[T]] = Future.reduce(futures)(_ ++ _)
}
