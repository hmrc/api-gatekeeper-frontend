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

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, Privileged, Ropc, Standard}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators, GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, _}
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.view.ApplicationViewModel
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails

trait ApplicationBuilder extends StateHistoryBuilder with CollaboratorsBuilder {

  // scalastyle:off parameter.number
  def buildApplication(
      id: ApplicationId = ApplicationId.random,
      clientId: ClientId = ClientId.random,
      gatewayId: String = "",
      name: Option[String] = None,
      deployedTo: Environment = Environment.SANDBOX,
      description: Option[String] = None,
      collaborators: Set[Collaborator] = Set.empty,
      createdOn: LocalDateTime = LocalDateTime.now(),
      lastAccess: Option[LocalDateTime] = Some(LocalDateTime.now()),
      grantLength: Int = GrantLength.EIGHTEEN_MONTHS.days,
      termsAndConditionsUrl: Option[String] = None,
      privacyPolicyUrl: Option[String] = None,
      access: Access = Standard(),
      state: ApplicationState = ApplicationState(State.PRODUCTION),
      rateLimitTier: RateLimitTier = RateLimitTier.BRONZE,
      checkInformation: Option[CheckInformation] = None,
      blocked: Boolean = false,
      ipAllowlist: IpAllowlist = IpAllowlist(),
      moreApplication: MoreApplication = MoreApplication()
    ): ApplicationResponse =
    ApplicationResponse(
      id,
      clientId,
      gatewayId,
      name.getOrElse(s"$id-name"),
      deployedTo,
      Some(description.getOrElse(s"$id-description")),
      collaborators,
      createdOn,
      lastAccess,
      grantLength,
      termsAndConditionsUrl,
      privacyPolicyUrl,
      access,
      state,
      rateLimitTier,
      checkInformation,
      blocked,
      ipAllowlist,
      moreApplication
    )
  // scalastyle:on parameter.number

  val DefaultApplication = buildApplication(
    collaborators = buildCollaborators(Seq(("a@b.com", CollaboratorRole.ADMINISTRATOR))),
    access = Standard(
      redirectUris = List("https://red1", "https://red2"),
      termsAndConditionsUrl = Some("http://tnc-url.com")
    )
  )

  def aCheckInformation2(): CheckInformation = {
    CheckInformation(
      contactDetails = Some(ContactDetails("contactFullName", "contactEmail", "contactTelephone")),
      confirmedName = true,
      providedPrivacyPolicyURL = true,
      providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details")
    )
  }

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
    ): ApplicationWithSubscriptionData = {
    ApplicationWithSubscriptionData(
      DefaultApplication,
      buildSubscriptions(apiContext, apiVersion),
      buildSubscriptionFieldValues(apiContext, apiVersion, fields)
    )
  }

  implicit class ApplicationViewModelExtension(applicationViewModel: ApplicationViewModel) {
    def withApplication(application: ApplicationResponse) = applicationViewModel.copy(application = application)

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
  }

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction        = applicationState.copy(name = State.PRODUCTION)
    def inTesting           = applicationState.copy(name = State.TESTING)
    def pendingGKApproval   = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: ApplicationResponse) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION)
    def deployedToSandbox    = app.copy(deployedTo = Environment.SANDBOX)

    def withoutCollaborator(email: LaxEmailAddress)         = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId)        = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)

    def withName(name: String)               = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAdmin(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Administrator(developer.userId, developer.email))
    }

    def withDeveloper(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Collaborators.Developer(developer.userId, developer.email))
    }

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard                 = app.copy(access = Standard())
    def asPrivileged               = app.copy(access = Privileged())
    def asROPC                     = app.copy(access = Ropc())

    def withState(state: ApplicationState) = app.copy(state = state)
    def inProduction                       = app.copy(state = app.state.inProduction)
    def inTesting                          = app.copy(state = app.state.inTesting)
    def pendingGKApproval                  = app.copy(state = app.state.pendingGKApproval)
    def pendingVerification                = app.copy(state = app.state.pendingVerification)

    def withBlocked(isBlocked: Boolean) = app.copy(blocked = isBlocked)
    def blocked                         = app.copy(blocked = true)
    def unblocked                       = app.copy(blocked = false)

    def withCheckInformation(checkInfo: CheckInformation) = app.copy(checkInformation = Some(checkInfo))
    def withEmptyCheckInformation                         = app.copy(checkInformation = Some(CheckInformation()))
    def noCheckInformation                                = app.copy(checkInformation = None)

    def withIpAllowlist(ipAllowlist: IpAllowlist) = app.copy(ipAllowlist = ipAllowlist)

    def withCreatedOn(createdOnDate: LocalDateTime)   = app.copy(createdOn = createdOnDate)
    def withLastAccess(lastAccessDate: LocalDateTime) = app.copy(lastAccess = Some(lastAccessDate))

    def withRateLimitTier(rateLimitTier: RateLimitTier) = app.copy(rateLimitTier = rateLimitTier)

  }
}
