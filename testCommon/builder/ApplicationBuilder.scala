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

import model.applications.NewApplication
import model.ApplicationId
import model.ClientId
import uk.gov.hmrc.time.DateTimeUtils
import model.Environment
import model.Standard
import model.State
import model.Collaborator
import model.applications.ApplicationWithSubscriptionData
import model.CollaboratorRole
import model.ApplicationState
import model.ApiIdentifier
import model.ApiVersion
import model.ApiContext
import model.SubscriptionFields.Fields
import model.FieldName
import model.FieldValue
import model.State.State
import model.StateHistory
import org.joda.time.DateTime
import java.{util => ju}
import model.Actor
import model.RateLimitTier
import model.CheckInformation
import model.Access
import model.view.ApplicationViewModel
import model.Privileged
import model.Ropc
import model.ApiStatus._
import model.User


trait ApplicationBuilder {

  def buildApplication(appId: ApplicationId = ApplicationId.random, createdOn: DateTime = DateTimeUtils.now, lastAccess: DateTime = DateTimeUtils.now, checkInformation: Option[CheckInformation] = None): NewApplication = {

    val clientId = ClientId.random
    val appOwnerEmail = "a@b.com"

    NewApplication(
      id = appId,
      clientId = clientId,
      gatewayId = "",
      name = s"$appId-name",
      createdOn = createdOn,
      lastAccess = lastAccess,
      lastAccessTokenUsage = None,
      deployedTo = Environment.SANDBOX,
      description = Some(s"$appId-description"),
      collaborators = buildCollaborators(Seq(appOwnerEmail)),
      state = ApplicationState(State.PRODUCTION),
      rateLimitTier = RateLimitTier.BRONZE,
      blocked = false,
      access = Standard(
        redirectUris = Seq("https://red1", "https://red2"),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      ),
      checkInformation = checkInformation
    )
  }

  val DefaultApplication = buildApplication()

  def buildCollaborators(emails: Seq[String]): Set[Collaborator] = {
    emails.map(email => Collaborator(email, CollaboratorRole.ADMINISTRATOR)).toSet
  }

  def buildSubscriptions(apiContext: ApiContext, apiVersion: ApiVersion): Set[ApiIdentifier] = 
    Set(
      ApiIdentifier(apiContext, apiVersion)
    )

  def buildSubscriptionFieldValues(apiContext: ApiContext, apiVersion: ApiVersion): Map[ApiContext, Map[ApiVersion, Fields.Alias]] = {
    val fields = Map(FieldName.random -> FieldValue.random, FieldName.random -> FieldValue.random)
    Map(apiContext -> Map(apiVersion -> fields))
  }

 def buildStateHistory(state: State, changedAt: DateTime = DateTimeUtils.now): StateHistory = {
    StateHistory(ju.UUID.randomUUID(), state, Actor("actor id"), None, changedAt)
  }

  def buildApplicationWithSubscriptionData(): ApplicationWithSubscriptionData = {
    val apiContext = ApiContext.random
    val apiVersion = ApiVersion.random

    ApplicationWithSubscriptionData(
      buildApplication(ApplicationId.random),
      buildSubscriptions(apiContext, apiVersion),
      buildSubscriptionFieldValues(apiContext, apiVersion)
    )
  }

  implicit class ApplicationViewModelExtension(applicationViewModel: ApplicationViewModel) {
    def withApplication(application: NewApplication) = applicationViewModel.copy(application = application)

    def withSubscriptions(subscriptions: Seq[(String, Seq[(ApiVersion, ApiStatus)])]) = applicationViewModel.copy(subscriptions = subscriptions)
    def withSubscriptionsThatHaveFieldDefns(subscriptions: Seq[(String, Seq[(ApiVersion, ApiStatus)])]) = applicationViewModel.copy(subscriptionsThatHaveFieldDefns = subscriptions)
    
    def asSuperUser = applicationViewModel.copy(isAtLeastSuperUser = true)
    def asAdmin = applicationViewModel.copy(isAdmin = true)
    
    def withDeveloper(developer: User) = {
      val newAppWithDev = this.applicationViewModel.application.withDeveloper(developer.email)
      applicationViewModel.copy(developers = List(developer), application = newAppWithDev)
    }
    def withAdmin(developer: User) = {
      val newAppWithDev = this.applicationViewModel.application.withAdmin(developer.email)
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
    def withAdmin(email: String) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + Collaborator(email, CollaboratorRole.ADMINISTRATOR))
    }
    def withDeveloper(email: String) = {
      val app1 = app.withoutCollaborator(email)
      app1.copy(collaborators = app1.collaborators + Collaborator(email, CollaboratorRole.DEVELOPER))
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

    def allowIPs(ips: String*) = app.copy(ipWhitelist = app.ipWhitelist ++ ips)

    def createdOn(createdOnDate: DateTime) = app.copy(createdOn = createdOnDate)
    def lastAccess(lastAccessDate: DateTime) = app.copy(lastAccess = lastAccessDate)
    
  }  
}
