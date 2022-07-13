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

package uk.gov.hmrc.gatekeeper.builder

import uk.gov.hmrc.gatekeeper.models.applications.NewApplication
import uk.gov.hmrc.gatekeeper.models._
import org.joda.time.DateTime
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.Fields
import org.joda.time.DateTime
import uk.gov.hmrc.gatekeeper.models.view.ApplicationViewModel
import uk.gov.hmrc.gatekeeper.models.ApiStatus._
import uk.gov.hmrc.gatekeeper.models.RateLimitTier.RateLimitTier

import java.time.Period

trait ApplicationBuilder extends StateHistoryBuilder with CollaboratorsBuilder {
  def buildApplication(appId: ApplicationId = ApplicationId.random, createdOn: DateTime = DateTime.now(), lastAccess: DateTime = DateTime.now(), checkInformation: Option[CheckInformation] = None): NewApplication = {
    val clientId = ClientId.random
    val appOwnerEmail = "a@b.com"
    val grantLength: Period = Period.ofDays(547)
    val ipAllowlist = IpAllowlist()

    NewApplication(
      id = appId,
      clientId = clientId,
      gatewayId = "",
      name = s"${appId.value}-name",
      createdOn = createdOn,
      lastAccess = Some(lastAccess),
      lastAccessTokenUsage = None,
      deployedTo = Environment.SANDBOX,
      description = Some(s"${appId.value}-description"),
      collaborators = buildCollaborators(Seq((appOwnerEmail, CollaboratorRole.ADMINISTRATOR))),
      state = ApplicationState(State.PRODUCTION),
      rateLimitTier = RateLimitTier.BRONZE,
      blocked = false,
      access = Standard(
        redirectUris = List("https://red1", "https://red2"),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      ),
      checkInformation = checkInformation,
      ipAllowlist = ipAllowlist,
      grantLength = grantLength
    )
  }

  val DefaultApplication = buildApplication()

  def buildSubscriptions(apiContext: ApiContext, apiVersion: ApiVersion): Set[ApiIdentifier] = 
    Set(
      ApiIdentifier(apiContext, apiVersion)
    )

  def buildSubscriptionFieldValues(apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias): Map[ApiContext, Map[ApiVersion, Fields.Alias]] = {
    Map(apiContext -> Map(apiVersion -> fields))
  }

  def buildApplicationWithSubscriptionData(apiContext: ApiContext = ApiContext.random,
                                          apiVersion: ApiVersion = ApiVersion.random,
                                          fields: Fields.Alias = Map(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)): ApplicationWithSubscriptionData = {
    ApplicationWithSubscriptionData(
      buildApplication(ApplicationId.random),
      buildSubscriptions(apiContext, apiVersion),
      buildSubscriptionFieldValues(apiContext, apiVersion, fields)
    )
  }

  implicit class ApplicationViewModelExtension(applicationViewModel: ApplicationViewModel) {
    def withApplication(application: NewApplication) = applicationViewModel.copy(application = application)

    def withSubscriptions(subscriptions: List[(String, List[(ApiVersion, ApiStatus)])]) = applicationViewModel.copy(subscriptions = subscriptions)
    def withSubscriptionsThatHaveFieldDefns(subscriptions: List[(String, List[(ApiVersion, ApiStatus)])]) = applicationViewModel.copy(subscriptionsThatHaveFieldDefns = subscriptions)
    
    def withDeveloper(developer: RegisteredUser) = {
      val newAppWithDev = this.applicationViewModel.application.withDeveloper(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }
    def withAdmin(developer: RegisteredUser) = {
      val newAppWithDev = this.applicationViewModel.application.withAdmin(developer)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }
  }

  implicit class ApplicationStateExtension(applicationState: ApplicationState) {
    def inProduction = applicationState.copy(name = State.PRODUCTION)
    def inTesting = applicationState.copy(name = State.TESTING)
    def pendingGKApproval = applicationState.copy(name = State.PENDING_GATEKEEPER_APPROVAL)
    def pendingVerification = applicationState.copy(name = State.PENDING_REQUESTER_VERIFICATION)
  }

  implicit class ApplicationExtension(app: NewApplication) {
    def deployedToProduction = app.copy(deployedTo = Environment.PRODUCTION)
    def deployedToSandbox = app.copy(deployedTo = Environment.SANDBOX)

    def withoutCollaborator(email: String) = app.copy(collaborators = app.collaborators.filterNot(c => c.emailAddress == email))
    def withCollaborators(collaborators: Set[Collaborator]) = app.copy(collaborators = collaborators)

    def withId(id: ApplicationId) = app.copy(id = id)
    def withClientId(clientId: ClientId) = app.copy(clientId = clientId)
    def withGatewayId(gatewayId: String) = app.copy(gatewayId = gatewayId)
    
    def withName(name: String) = app.copy(name = name)
    def withDescription(description: String) = app.copy(description = Some(description))

    def withAdmin(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR, developer.userId))
    }
    def withDeveloper(developer: RegisteredUser) = {
      val app1 = app.withoutCollaborator(developer.email)
      app1.copy(collaborators = app1.collaborators + Collaborator(developer.email, CollaboratorRole.DEVELOPER, developer.userId))
    }

    def withAccess(access: Access) = app.copy(access = access)
    def asStandard = app.copy(access = Standard())
    def asPrivileged = app.copy(access = Privileged())
    def asROPC = app.copy(access = Ropc())

    def withState(state: ApplicationState) = app.copy(state = state)
    def inProduction = app.copy(state = app.state.inProduction)
    def inTesting = app.copy(state = app.state.inTesting)
    def pendingGKApproval = app.copy(state = app.state.pendingGKApproval)
    def pendingVerification = app.copy(state = app.state.pendingVerification)

    def withBlocked(isBlocked: Boolean) = app.copy(blocked = isBlocked)
    def blocked = app.copy(blocked = true)
    def unblocked = app.copy(blocked = false)

    def withCheckInformation(checkInfo: CheckInformation) = app.copy(checkInformation = Some(checkInfo))
    def withEmptyCheckInformation = app.copy(checkInformation = Some(CheckInformation()))
    def noCheckInformation = app.copy(checkInformation = None)

    def withIpAllowlist(ipAllowlist: IpAllowlist) = app.copy(ipAllowlist = ipAllowlist)

    def withCreatedOn(createdOnDate: DateTime) = app.copy(createdOn = createdOnDate)
    def withLastAccess(lastAccessDate: DateTime) = app.copy(lastAccess = Some(lastAccessDate))

    def withRateLimitTier(rateLimitTier: RateLimitTier) = app.copy(rateLimitTier = rateLimitTier)
    
  }  
}
