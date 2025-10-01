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

package uk.gov.hmrc.gatekeeper.builder

import java.time.Instant

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiStatus
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators.Administrator
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.view.ApplicationViewModel
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails

trait ApplicationBuilder extends StateHistoryBuilder with CollaboratorsBuilder with FixedClock with ApplicationWithCollaboratorsFixtures with ApplicationTokenFixtures {

  // scalastyle:off parameter.number
  def buildApplication(
      id: ApplicationId = ApplicationId.random,
      clientId: ClientId = ClientId.random,
      gatewayId: String = "",
      name: Option[String] = None,
      deployedTo: Environment = Environment.SANDBOX,
      description: Option[String] = None,
      collaborators: Set[Collaborator] = Set.empty,
      createdOn: Instant = instant,
      lastAccess: Option[Instant] = Some(instant),
      grantLength: GrantLength = GrantLength.EIGHTEEN_MONTHS,
      access: Access = Access.Standard(),
      state: ApplicationState = ApplicationState(State.PRODUCTION, updatedOn = instant),
      rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
      checkInformation: Option[CheckInformation] = None,
      blocked: Boolean = false,
      ipAllowlist: IpAllowlist = IpAllowlist(),
      loginRedirectUris: List[LoginRedirectUri] = List.empty,
      postLogoutRedirectUris: List[PostLogoutRedirectUri] = List.empty,
      deleteRestriction: DeleteRestriction = DeleteRestriction.NoRestriction,
      lastActionActor: ActorType = ActorType.UNKNOWN
    ): ApplicationWithCollaborators = {

    val access2 = access match {
      case a: Access.Standard => a.copy(redirectUris = loginRedirectUris, postLogoutRedirectUris = postLogoutRedirectUris)
      case _                  => access
    }

    ApplicationWithCollaborators(
      CoreApplication(
        id,
        applicationTokenOne.copy(clientId = clientId, lastAccessTokenUsage = lastAccess),
        gatewayId,
        ApplicationName(name.getOrElse(s"${id.value}-name")),
        deployedTo,
        Some(description.getOrElse(s"${id.value}-description")),
        createdOn,
        lastAccess,
        grantLength,
        access2,
        state,
        rateLimitTier,
        checkInformation,
        blocked,
        ipAllowlist,
        lastActionActor,
        deleteRestriction,
        None
      ),
      collaborators
    )
  }
  // scalastyle:on parameter.number

  val DefaultApplication = buildApplication(
    collaborators = buildCollaborators(Seq(("a@b.com", Collaborator.Roles.ADMINISTRATOR))),
    access = Access.Standard(
      redirectUris = List("https://red1", "https://red2").map(LoginRedirectUri.unsafeApply),
      termsAndConditionsUrl = Some("http://tnc-url.com")
    )
  )

  def anApplicationWithHistory(applicationResponse: ApplicationWithCollaborators = anApplication(), stateHistories: List[StateHistory] = List.empty): ApplicationWithHistory = {
    ApplicationWithHistory(applicationResponse, stateHistories)
  }

  def anApplication(createdOn: Instant = instant, lastAccess: Instant = instant): ApplicationWithCollaborators = {
    buildApplication(
      ApplicationId.random,
      ClientId("clientid"),
      "gatewayId",
      Some("appName"),
      Environment.PRODUCTION,
      None,
      Set.empty,
      createdOn,
      Some(lastAccess),
      access = Access.Privileged(),
      state = ApplicationState(updatedOn = instant)
    )
  }

  def anApplicationResponseWith(checkInformation: CheckInformation): ApplicationWithCollaborators = {
    anApplication().modify(_.copy(checkInformation = Some(checkInformation)))
  }

  def aCheckInformation(): CheckInformation = {
    CheckInformation(
      contactDetails = Some(ContactDetails(FullName("contactFullName"), LaxEmailAddress("contactEmail"), "contactTelephone")),
      confirmedName = true,
      providedPrivacyPolicyURL = true,
      providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details")
    )
  }

  def aStateHistory(state: State, changedAt: Instant = instant): StateHistory = {
    StateHistory(ApplicationId.random, state, anActor, changedAt = changedAt)
  }

  val anActor = Actors.Unknown

  val aDeleteRestriction = DeleteRestriction.DoNotDelete("Kept for testing", anActor, instant)

  def buildSubscriptions(apiContext: ApiContext, apiVersion: ApiVersionNbr): Set[ApiIdentifier] =
    Set(
      ApiIdentifier(apiContext, apiVersion)
    )

  def buildSubscriptionFieldValues(apiContext: ApiContext, apiVersion: ApiVersionNbr, fields: Fields.Alias): Map[ApiContext, Map[ApiVersionNbr, Fields.Alias]] = {
    Map(apiContext -> Map(apiVersion -> fields))
  }

  def buildApplicationWithSubscriptionData(
      apiContext: ApiContext = ApiContext.random,
      apiVersion: ApiVersionNbr = ApiVersionNbr.random,
      fields: Fields.Alias = Map(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    ): ApplicationWithSubscriptionFields = {
    ApplicationWithSubscriptionFields(
      DefaultApplication.details,
      DefaultApplication.collaborators,
      buildSubscriptions(apiContext, apiVersion),
      buildSubscriptionFieldValues(apiContext, apiVersion, fields)
    )
  }

  implicit class ApplicationViewModelExtension(applicationViewModel: ApplicationViewModel) {
    def withApplication(application: ApplicationWithCollaborators) = applicationViewModel.copy(application = application)

    def withSubscriptions(subscriptions: List[(String, List[(ApiVersionNbr, ApiStatus)])]) = applicationViewModel.copy(subscriptions = subscriptions)

    def withSubscriptionsThatHaveFieldDefns(subscriptions: List[(String, List[(ApiVersionNbr, ApiStatus)])]) =
      applicationViewModel.copy(subscriptionsThatHaveFieldDefns = subscriptions)

    def withDeveloper(developer: RegisteredUser) = {
      val newAppWithDev = this.applicationViewModel.application.withDeveloper(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }

    def withAdmin(developer: RegisteredUser) = {
      val newAppWithDev = this.applicationViewModel.application.withAdmin(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }

    def withMaybeLatestTOUAgreement(maybeLatestTOUAgreement: Option[TermsOfUseAgreementDisplayDetails]) =
      applicationViewModel.copy(maybeLatestTOUAgreement = maybeLatestTOUAgreement)

    def withHasSubmissions(hasSubmissions: Boolean) = applicationViewModel.copy(hasSubmissions = hasSubmissions)
  }

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction        = applicationState.copy(name = State.PRODUCTION)
    def inTesting           = applicationState.copy(name = State.TESTING)
    def pendingGKApproval   = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: ApplicationWithCollaborators) {
    def deployedToProduction = app.withEnvironment(Environment.PRODUCTION)
    def deployedToSandbox    = app.withEnvironment(Environment.SANDBOX)

    def withoutCollaborator(email: LaxEmailAddress)         = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId)        = app.modify(_.copy(id = id))
    def withClientId(clientId: ClientId) = app.modify(_.copy(token = app.details.token.copy(clientId = clientId)))
    def withGatewayId(gatewayId: String) = app.modify(_.copy(gatewayId = gatewayId))

    def withName(name: ApplicationName)      = app.modify(_.copy(name = name))
    def withDescription(description: String) = app.modify(_.copy(description = Some(description)))

    def withAdmin(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Administrator(developer.userId, developer.email))
    }

    def withDeveloper(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Collaborators.Developer(developer.userId, developer.email))
    }

    def withAccess(access: Access) = app.withAccess(access)
    def asStandard                 = app.withAccess(Access.Standard())
    def asPrivileged               = app.withAccess(Access.Privileged())
    def asROPC                     = app.withAccess(Access.Ropc())

    def withState(state: ApplicationState) = app.withState(state)

    def withBlocked(isBlocked: Boolean) = app.modify(_.copy(blocked = isBlocked))
    def blocked                         = app.modify(_.copy(blocked = true))
    def unblocked                       = app.modify(_.copy(blocked = false))

    def withCheckInformation(checkInfo: CheckInformation) = app.modify(_.copy(checkInformation = Some(checkInfo)))
    def withEmptyCheckInformation                         = app.modify(_.copy(checkInformation = Some(CheckInformation())))
    def noCheckInformation                                = app.modify(_.copy(checkInformation = None))

    def withIpAllowlist(ipAllowlist: IpAllowlist) = app.modify(_.copy(ipAllowlist = ipAllowlist))

    def withCreatedOn(createdOnDate: Instant)   = app.modify(_.copy(createdOn = createdOnDate))
    def withLastAccess(lastAccessDate: Instant) = app.modify(_.copy(lastAccess = Some(lastAccessDate)))

    def withRateLimitTier(rateLimitTier: RateLimitTier) = app.modify(_.copy(rateLimitTier = rateLimitTier))

    def toSeq = Seq(app)
  }
}
