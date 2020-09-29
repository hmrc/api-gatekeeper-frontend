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

package builder

import java.util.UUID._

import model.State._
import model._
import model.RateLimitTier.RateLimitTier
import org.joda.time.DateTime
import uk.gov.hmrc.time.DateTimeUtils

trait ApplicationResponseBuilder extends CollaboratorsBuilder {
    def buildApplicationResponse(appId: ApplicationId = ApplicationId.random, createdOn: DateTime = DateTimeUtils.now, lastAccess: DateTime = DateTimeUtils.now, checkInformation: Option[CheckInformation] = None): ApplicationResponse = {

    val clientId = ClientId.random
    val appOwnerEmail = "a@b.com"

    ApplicationResponse(
      id = appId,
      clientId = clientId,
      gatewayId = "",
      name = s"$appId-name",
      deployedTo = Environment.SANDBOX.toString,
      description = Some(s"$appId-description"),
      collaborators = buildCollaborators(Seq((appOwnerEmail, CollaboratorRole.ADMINISTRATOR))),
      createdOn = createdOn,
      lastAccess = lastAccess,
      access = Standard(
        redirectUris = Seq("https://red1", "https://red2"),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      ),
      state = ApplicationState(State.PRODUCTION),
      rateLimitTier = RateLimitTier.BRONZE,
      termsAndConditionsUrl = Some("http://tnc-url.com"),
      privacyPolicyUrl = Some("http://privacy-policy-url.com"),
      checkInformation = checkInformation,
      blocked = false,
      ipWhitelist = Set.empty
    )
  }

  val DefaultApplicationResponse = buildApplicationResponse()

  def anApplicationWithHistory(applicationResponse: ApplicationResponse = anApplicationResponse(),
                               stateHistories: Seq[StateHistory] = Seq.empty): ApplicationWithHistory = {
    ApplicationWithHistory(applicationResponse, stateHistories)
  }

  def anApplicationResponse(createdOn: DateTime = DateTimeUtils.now, lastAccess: DateTime = DateTimeUtils.now): ApplicationResponse = {
    ApplicationResponse(ApplicationId(randomUUID().toString), ClientId("clientid"), "gatewayId", "appName", "deployedTo", None, Set.empty, createdOn,
      lastAccess, Privileged(), ApplicationState(), RateLimitTier.BRONZE, Some("termsUrl"), Some("privacyPolicyUrl"), None)
  }

  def anApplicationResponseWith(checkInformation: CheckInformation): ApplicationResponse = {
    anApplicationResponse().copy(checkInformation = Some(checkInformation))
  }

  def aCheckInformation(): CheckInformation = {
    CheckInformation(contactDetails = Some(ContactDetails("contactFullName", "contactEmail", "contactTelephone")),
      confirmedName = true, providedPrivacyPolicyURL = true, providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details"))
  }

  def aStateHistory(state: State, changedAt: DateTime = DateTimeUtils.now): StateHistory = {
    StateHistory(ApplicationId.random, state, anActor(), None, changedAt)
  }

  def anActor() = Actor("actor id")

  implicit class ApplicationResponseExtension(app: ApplicationResponse) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION.toString)
    def deployedToSandbox = app.copy(deployedTo = Environment.SANDBOX.toString)

    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId) = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)
    
    def withName(name: String) = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard = app.copy(access = Standard())
    def asPrivileged = app.copy(access = Privileged())
    def asROPC = app.copy(access = Ropc())

    def withState(state: ApplicationState) = app.copy(state = state) 

    def withBlocked(isBlocked: Boolean) = app.copy(blocked = isBlocked)
    def blocked = app.copy(blocked = true)
    def unblocked = app.copy(blocked = false)

    def withCheckInformation(checkInfo: CheckInformation) = app.copy(checkInformation = Some(checkInfo))
    def withEmptyCheckInformation = app.copy(checkInformation = Some(CheckInformation()))
    def noCheckInformation = app.copy(checkInformation = None)

    def allowIPs(ips: String*) = app.copy(ipWhitelist = app.ipWhitelist ++ ips)

    def withCreatedOn(createdOnDate: DateTime) = app.copy(createdOn = createdOnDate)
    def withLastAccess(lastAccessDate: DateTime) = app.copy(lastAccess = lastAccessDate)

    def withRateLimitTier(rateLimitTier: RateLimitTier) = app.copy(rateLimitTier = rateLimitTier)

    def toSeq = Seq(app)
  } 
}
