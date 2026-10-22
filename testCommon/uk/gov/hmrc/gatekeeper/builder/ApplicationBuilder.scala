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
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.*
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.Collaborators.Administrator
import uk.gov.hmrc.apiplatform.modules.common.domain.models.*
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.*
import uk.gov.hmrc.gatekeeper.models.*
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields
import uk.gov.hmrc.gatekeeper.models.view.ApplicationViewModel
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails

trait ApplicationBuilder extends StateHistoryBuilder with CollaboratorsBuilder with FixedClock with ApplicationWithCollaboratorsFixtures with ApplicationTokenFixtures {

  def buildApplication(
      id: ApplicationId = ApplicationId.random,
      clientId: ClientId = ClientId.random,
      name: Option[String] = None,
      deployedTo: Environment = Environment.Sandbox,
      description: Option[String] = None,
      collaborators: Set[Collaborator] = Set.empty,
      createdOn: Instant = instant,
      lastAccess: Option[Instant] = Some(instant),
      lastAccessTokenUsage: Option[Instant] = Some(instant),
      grantLength: GrantLength = GrantLength.EIGHTEEN_MONTHS,
      access: Access = Access.Standard(),
      state: ApplicationState = ApplicationState(State.Production, updatedOn = instant),
      rateLimitTier: RateLimitTier = RateLimitTier.Bronze,
      checkInformation: Option[CheckInformation] = None,
      blocked: Boolean = false,
      ipAllowlist: IpAllowlist = IpAllowlist(),
      loginRedirectUris: List[LoginRedirectUri] = List.empty,
      postLogoutRedirectUris: List[PostLogoutRedirectUri] = List.empty,
      deleteRestriction: DeleteRestriction = DeleteRestriction.NoRestriction,
      lastActionActor: ActorType = ActorType.Unknown
    ): ApplicationWithCollaborators = {

    val access2 = access match {
      case a: Access.Standard => a.copy(redirectUris = loginRedirectUris, postLogoutRedirectUris = postLogoutRedirectUris)
      case _                  => access
    }

    ApplicationWithCollaborators(
      CoreApplication(
        id,
        applicationTokenOne.copy(clientId = clientId, lastAccessTokenUsage = lastAccessTokenUsage),
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

  val DefaultApplication = buildApplication(
    collaborators = buildCollaborators(Seq(("a@b.com", Collaborator.Role.Administrator))),
    access = Access.Standard(
      redirectUris = List("https://red1", "https://red2").map(LoginRedirectUri.unsafeApply),
      termsAndConditionsUrl = Some("http://tnc-url.com")
    )
  )

  def anApplication(createdOn: Instant = instant, lastAccess: Instant = instant): ApplicationWithCollaborators = {
    buildApplication(
      ApplicationId.random,
      ClientId("clientid"),
      Some("appName"),
      Environment.Production,
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

  def buildAppWithSubsFields(
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

  extension (applicationState: ApplicationState) {
    def inProduction        = applicationState.copy(name = State.Production)
    def inTesting           = applicationState.copy(name = State.Testing)
    def pendingGKApproval   = applicationState.copy(name = State.PendingGatekeeperApproval)
    def pendingVerification = applicationState.copy(name = State.PendingRequesterVerification)
  }

  extension (app: ApplicationWithCollaborators) {
    def deployedToProduction: ApplicationWithCollaborators                                = app.withEnvironment(Environment.Production)
    def deployedToSandbox: ApplicationWithCollaborators                                   = app.withEnvironment(Environment.Sandbox)
    //
    def withoutCollaborator(email: LaxEmailAddress): ApplicationWithCollaborators         = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]): ApplicationWithCollaborators = app.copy(collaborators = collaborators)
    //
    def withId(id: ApplicationId): ApplicationWithCollaborators                           = app.modify(_.copy(id = id))
    def withClientId(clientId: ClientId): ApplicationWithCollaborators                    = app.modify(_.copy(token = app.details.token.copy(clientId = clientId)))
    //
    def withName(name: ApplicationName): ApplicationWithCollaborators                     = app.modify(_.copy(name = name))
    def withDescription(description: String): ApplicationWithCollaborators                = app.modify(_.copy(description = Some(description)))

    //
    def withAdmin(developer: RegisteredUser): ApplicationWithCollaborators = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Administrator(developer.userId, developer.email))
    }

    //
    def withDeveloper(developer: RegisteredUser): ApplicationWithCollaborators          = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Collaborators.Developer(developer.userId, developer.email))
    }
    //
    def asStandard: ApplicationWithCollaborators                                        = app.withAccess(Access.Standard())
    def asPrivileged: ApplicationWithCollaborators                                      = app.withAccess(Access.Privileged())
    def asROPC: ApplicationWithCollaborators                                            = app.withAccess(Access.Ropc())
    //
    def withBlocked(isBlocked: Boolean): ApplicationWithCollaborators                   = app.modify(_.copy(blocked = isBlocked))
    def blocked: ApplicationWithCollaborators                                           = app.modify(_.copy(blocked = true))
    def unblocked: ApplicationWithCollaborators                                         = app.modify(_.copy(blocked = false))
    //
    def withCheckInformation(checkInfo: CheckInformation): ApplicationWithCollaborators = app.modify(_.copy(checkInformation = Some(checkInfo)))
    def withEmptyCheckInformation: ApplicationWithCollaborators                         = app.modify(_.copy(checkInformation = Some(CheckInformation())))
    def noCheckInformation: ApplicationWithCollaborators                                = app.modify(_.copy(checkInformation = None))
    //
    def withIpAllowlist(ipAllowlist: IpAllowlist): ApplicationWithCollaborators         = app.modify(_.copy(ipAllowlist = ipAllowlist))
    //
    def withCreatedOn(createdOnDate: Instant): ApplicationWithCollaborators             = app.modify(_.copy(createdOn = createdOnDate))
    def withLastAccess(lastAccessDate: Instant): ApplicationWithCollaborators           = app.modify(_.copy(lastAccess = Some(lastAccessDate)))
    //
    def withRateLimitTier(rateLimitTier: RateLimitTier): ApplicationWithCollaborators   = app.modify(_.copy(rateLimitTier = rateLimitTier))

    def toSeq = Seq(app)
  }

  extension (applicationViewModel: ApplicationViewModel) {
    def withApplication(application: ApplicationWithCollaborators): ApplicationViewModel = applicationViewModel.copy(application = application)

    def withSubscriptions(subscriptions: List[(String, List[(ApiVersionNbr, ApiStatus)])]): ApplicationViewModel = applicationViewModel.copy(subscriptions = subscriptions)

    def withSubscriptionsThatHaveFieldDefns(subscriptions: List[(String, List[(ApiVersionNbr, ApiStatus)])]): ApplicationViewModel =
      applicationViewModel.copy(subscriptionsThatHaveFieldDefns = subscriptions)

    def withDeveloper(developer: RegisteredUser): ApplicationViewModel = {
      val newAppWithDev = applicationViewModel.application.withDeveloper(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }

    def withAdmin(developer: RegisteredUser): ApplicationViewModel = {
      val newAppWithDev = applicationViewModel.application.withAdmin(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }

    def withMaybeLatestTOUAgreement(maybeLatestTOUAgreement: Option[TermsOfUseAgreementDisplayDetails]): ApplicationViewModel =
      applicationViewModel.copy(maybeLatestTOUAgreement = maybeLatestTOUAgreement)

    def withHasSubmissions(hasSubmissions: Boolean): ApplicationViewModel = applicationViewModel.copy(hasSubmissions = hasSubmissions)
  }
}
