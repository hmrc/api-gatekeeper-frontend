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

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, Privileged, Ropc, Standard}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.State.State
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, _}
import uk.gov.hmrc.gatekeeper.models._

trait ApplicationResponseBuilder extends CollaboratorsBuilder {

  // scalastyle:off parameter.number
  def buildApplicationResponse(
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
      access: Access,
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

  val DefaultApplicationResponse = buildApplicationResponse(
    collaborators = buildCollaborators(Seq(("a@b.com", CollaboratorRole.ADMINISTRATOR))),
    termsAndConditionsUrl = Some("http://tnc-url.com"),
    privacyPolicyUrl = Some("http://privacy-policy-url.com"),
    access = Standard(
      redirectUris = List("https://red1", "https://red2"),
      termsAndConditionsUrl = Some("http://tnc-url.com")
    )
  )

  def anApplicationWithHistory(applicationResponse: ApplicationResponse = anApplicationResponse(), stateHistories: List[StateHistory] = List.empty): ApplicationWithHistory = {
    ApplicationWithHistory(applicationResponse, stateHistories)
  }

  def anApplicationResponse(createdOn: LocalDateTime = LocalDateTime.now(), lastAccess: LocalDateTime = LocalDateTime.now()): ApplicationResponse = {
    buildApplicationResponse(
      ApplicationId.random,
      ClientId("clientid"),
      "gatewayId",
      Some("appName"),
      Environment.PRODUCTION,
      None,
      Set.empty,
      createdOn,
      Some(lastAccess),
      termsAndConditionsUrl = Some("termsUrl"),
      privacyPolicyUrl = Some("privacyPolicyUrl"),
      access = Privileged(),
      state = ApplicationState()
    )
  }

  def anApplicationResponseWith(checkInformation: CheckInformation): ApplicationResponse = {
    anApplicationResponse().copy(checkInformation = Some(checkInformation))
  }

  def aCheckInformation(): CheckInformation = {
    CheckInformation(
      contactDetails = Some(ContactDetails("contactFullName", "contactEmail", "contactTelephone")),
      confirmedName = true,
      providedPrivacyPolicyURL = true,
      providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details")
    )
  }

  def aStateHistory(state: State, changedAt: LocalDateTime = LocalDateTime.now()): StateHistory = {
    StateHistory(ApplicationId.random, state, anActor(), None, changedAt)
  }

  def anActor() = Actors.Unknown

  implicit class ApplicationResponseExtension(app: ApplicationResponse) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION)
    def deployedToSandbox    = app.copy(deployedTo = Environment.SANDBOX)

    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId)        = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)

    def withName(name: String)               = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard                 = app.copy(access = Standard())
    def asPrivileged               = app.copy(access = Privileged())
    def asROPC                     = app.copy(access = Ropc())

    def withState(state: ApplicationState) = app.copy(state = state)

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

    def toSeq = Seq(app)
  }
}
