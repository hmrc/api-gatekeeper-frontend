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
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._
import org.mockito.captor.ArgCaptor
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gatekeeper.utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import org.mockito.scalatest.ResetMocksAfterEachTest
import uk.gov.hmrc.http.UpstreamErrorResponse
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import mocks.connectors.ApplicationConnectorMockProvider
import mocks.connectors.ApmConnectorMockProvider
import mocks.services.ApiScopeConnectorMockProvider
import org.joda.time.DateTime
import uk.gov.hmrc.gatekeeper.models.State.State
import java.time.{LocalDateTime, Period}

class ApplicationServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest {

  trait Setup
      extends MockitoSugar with ArgumentMatchersSugar
      with ApplicationConnectorMockProvider
      with ApmConnectorMockProvider
      with ApiScopeConnectorMockProvider {
    val mockDeveloperConnector = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val applicationService = new ApplicationService(
      mockSandboxApplicationConnector,
      mockProductionApplicationConnector,
      mockSandboxApiScopeConnector,
      mockProductionApiScopeConnector,
      mockApmConnector,
      mockDeveloperConnector,
      mockSubscriptionFieldsService)
    val underTest = spy(applicationService)

    implicit val hc = HeaderCarrier()

    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER, UserId.random))

    val grantLength: Period = Period.ofDays(547)
    val stdApp1 = ApplicationResponse(
      ApplicationId.random, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)
    val stdApp2 = ApplicationResponse(
      ApplicationId.random, ClientId("clientid2"), "gatewayId2", "application2", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Standard(), ApplicationState(), grantLength)
    val privilegedApp = ApplicationResponse(
      ApplicationId.random, ClientId("clientid3"), "gatewayId3", "application3", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Privileged(), ApplicationState(), grantLength)
    val ropcApp = ApplicationResponse(
      ApplicationId.random, ClientId("clientid4"), "gatewayId4", "application4", "PRODUCTION", None, collaborators, DateTime.now(), Some(DateTime.now()), Ropc(), ApplicationState(), grantLength)
    val applicationWithHistory = ApplicationWithHistory(stdApp1, List.empty)
    val gatekeeperUserId = "loggedin.gatekeeper"

    val apiIdentifier = ApiIdentifier(ApiContext.random, ApiVersion.random)

    val context = apiIdentifier.context
    val version = apiIdentifier.version

    val allProductionApplications = List(stdApp1, stdApp2, privilegedApp)
    val allSandboxApplications = allProductionApplications.map(_.copy(id = ApplicationId.random, deployedTo = "SANDBOX"))
    val testContext = ApiContext("test-context")
    val unknownContext = ApiContext("unknown-context")
    val superContext = ApiContext("super-context")
    val sandboxTestContext = ApiContext("sandbox-test-context")
    val sandboxUnknownContext = ApiContext("sandbox-unknown-context")
    val sandboxSuperContext = ApiContext("sandbox-super-context")
    val subscriptionFieldDefinition = SubscriptionFieldDefinition(FieldName.random, "description", "hint", "String", "shortDescription")
    val prefetchedDefinitions : DefinitionsByApiVersion = Map(apiIdentifier -> List(subscriptionFieldDefinition))
    val definitions = List(subscriptionFieldDefinition)
  }

  trait SubscriptionFieldsServiceSetup extends Setup {

    def subscriptionFields : List[SubscriptionFieldValue]

  }

  "searchApplications" should {

    "list all subscribed applications from production when PRODUCTION environment is specified" in new Setup {
      ApplicationConnectorMock.Prod.SearchApplications.returns(allProductionApplications: _*)

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(PRODUCTION), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1 shouldBe stdApp1
      app2 shouldBe stdApp2
      app3 shouldBe privilegedApp
    }

    "list all subscribed applications from sandbox when SANDBOX environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.SearchApplications.returns(allSandboxApplications: _*)
      
      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(SANDBOX), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.deployedTo shouldBe "SANDBOX"
      app2.deployedTo shouldBe "SANDBOX"
      app3.deployedTo shouldBe "SANDBOX"
    }

    "list all subscribed applications from sandbox when no environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.SearchApplications.returns(allSandboxApplications: _*)

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(None, Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.deployedTo shouldBe "SANDBOX"
      app2.deployedTo shouldBe "SANDBOX"
      app3.deployedTo shouldBe "SANDBOX"
    }
  }

  "resendVerification" should {

    "call applicationConnector with appropriate parameters" in new Setup {
      val userName = "userName"

      val gatekeeperIdCaptor = ArgCaptor[String]
      val appIdCaptor = ArgCaptor[ApplicationId]

      when(mockProductionApplicationConnector.resendVerification(appIdCaptor, gatekeeperIdCaptor)(*))
        .thenReturn(successful(ResendVerificationSuccessful))

      await(underTest.resendVerification(stdApp1, userName))
      gatekeeperIdCaptor.value shouldBe userName
      appIdCaptor.value shouldBe stdApp1.id
    }
  }

  "fetchProdAppStateHistories" should {
    def buildAppStateHistories(states: State*) =
      ApplicationStateHistory(ApplicationId.random, "app name", 2, states.toList.map(ApplicationStateHistoryItem(_, LocalDateTime.now)))

    "handle apps with no state history correctly" in new Setup {
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(
        buildAppStateHistories()
      )
      val result = await(underTest.fetchProdAppStateHistories)
      result shouldEqual List()
    }

    "handle apps with single state history item correctly" in new Setup {
      val appStateHistory = buildAppStateHistories(State.TESTING)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(appStateHistory)

      val result = await(underTest.fetchProdAppStateHistories)

      result shouldEqual List(
        ApplicationStateHistoryChange(appStateHistory.applicationId.value, appStateHistory.appName, appStateHistory.journeyVersion.toString, "TESTING", appStateHistory.stateHistory(0).timestamp.toString, "", "")
      )
    }

    "handle apps with multiple state history items correctly" in new Setup {
      val appStateHistory = buildAppStateHistories(State.TESTING, State.PENDING_GATEKEEPER_APPROVAL, State.PRODUCTION)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(appStateHistory)

      val result = await(underTest.fetchProdAppStateHistories)

      result shouldEqual List(
        ApplicationStateHistoryChange(appStateHistory.applicationId.value, appStateHistory.appName, appStateHistory.journeyVersion.toString, "TESTING", appStateHistory.stateHistory(0).timestamp.toString, "PENDING_GATEKEEPER_APPROVAL", appStateHistory.stateHistory(1).timestamp.toString),
        ApplicationStateHistoryChange(appStateHistory.applicationId.value, appStateHistory.appName, appStateHistory.journeyVersion.toString, "PENDING_GATEKEEPER_APPROVAL", appStateHistory.stateHistory(1).timestamp.toString, "PRODUCTION", appStateHistory.stateHistory(2).timestamp.toString),
        ApplicationStateHistoryChange(appStateHistory.applicationId.value, appStateHistory.appName, appStateHistory.journeyVersion.toString, "PRODUCTION", appStateHistory.stateHistory(2).timestamp.toString, "", "")
      )
    }
    "handle multiple apps in response correctly" in new Setup {
      val app1StateHistory = buildAppStateHistories(State.TESTING)
      val app2StateHistory = buildAppStateHistories(State.TESTING, State.PRODUCTION)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(app1StateHistory, app2StateHistory)

      val result = await(underTest.fetchProdAppStateHistories)

      result shouldEqual List(
        ApplicationStateHistoryChange(app1StateHistory.applicationId.value, app1StateHistory.appName, app1StateHistory.journeyVersion.toString, "TESTING", app1StateHistory.stateHistory(0).timestamp.toString, "", ""),
        ApplicationStateHistoryChange(app2StateHistory.applicationId.value, app2StateHistory.appName, app2StateHistory.journeyVersion.toString, "TESTING", app2StateHistory.stateHistory(0).timestamp.toString, "PRODUCTION", app2StateHistory.stateHistory(1).timestamp.toString),
        ApplicationStateHistoryChange(app2StateHistory.applicationId.value, app2StateHistory.appName, app2StateHistory.journeyVersion.toString, "PRODUCTION", app2StateHistory.stateHistory(1).timestamp.toString, "", ""),
      )
    }
  }

  "fetchApplications" should {

    "list all applications from sandbox and production when filtering not provided" in new Setup {
      ApplicationConnectorMock.Prod.FetchAllApplications.returns(allProductionApplications: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplications.returns(allProductionApplications: _*)

      val result: Seq[ApplicationResponse] = await(underTest.fetchApplications)
      result shouldEqual allProductionApplications

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list filtered applications from sandbox and production when specific subscription filtering is provided" in new Setup {
      val filteredApplications = List(stdApp1, privilegedApp)

      ApplicationConnectorMock.Prod.FetchAllApplicationsBySubscription.returns(filteredApplications: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplicationsBySubscription.returns(filteredApplications: _*)

      val result = await(underTest.fetchApplications(Value("subscription", "version"), AnyEnvironment))
      result shouldBe filteredApplications

      verify(mockProductionApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
    }

    "list filtered applications from sandbox and production when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(stdApp1, privilegedApp)
      val subscriptions = List(stdApp2, ropcApp)

      val allApps = noSubscriptions ++ subscriptions
      ApplicationConnectorMock.Prod.FetchAllApplications.returns(allApps: _*)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithNoSubscriptions.returns(noSubscriptions: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplications.returns(allApps: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplicationsWithNoSubscriptions.returns(noSubscriptions: _*)

      val result = await(underTest.fetchApplications(OneOrMoreSubscriptions, AnyEnvironment))
      result shouldBe subscriptions

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
    }

    "list filtered applications from sandbox and production when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = List(stdApp1, privilegedApp)

      ApplicationConnectorMock.Prod.FetchAllApplications.returns(allApps: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplications.returns()

      val result = await(underTest.fetchApplications(OneOrMoreApplications, AnyEnvironment))
      result shouldBe allApps

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list distinct filtered applications from sandbox and production when NoSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(stdApp1, privilegedApp)

     ApplicationConnectorMock.Prod.FetchAllApplicationsWithNoSubscriptions.returns(noSubscriptions: _*)
     ApplicationConnectorMock.Sandbox.FetchAllApplicationsWithNoSubscriptions.returns(noSubscriptions: _*)

      val result = await(underTest.fetchApplications(NoSubscriptions, AnyEnvironment))
      result shouldBe noSubscriptions

      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
    }
  }

  "fetchApplication" should {
    "return the app when found in production" in new Setup {
      ApplicationConnectorMock.Prod.FetchApplication.returns(applicationWithHistory)
      ApplicationConnectorMock.Sandbox.FetchApplication.failsNotFound()

      val result = await(underTest.fetchApplication(stdApp1.id))

      result shouldBe applicationWithHistory

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
      verify(mockSandboxApplicationConnector, never).fetchApplication(*[ApplicationId])(*)
    }

    "return the the app in sandbox when not found in production" in new Setup {
      ApplicationConnectorMock.Prod.FetchApplication.failsNotFound()
      ApplicationConnectorMock.Sandbox.FetchApplication.returns(applicationWithHistory)

      await(underTest.fetchApplication(stdApp1.id))

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
      verify(mockSandboxApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
    }
  }

  "updateOverrides" should {
    "call the service to update the overrides for an app with Standard access" in new Setup {
      when(mockProductionApplicationConnector.updateOverrides(*[ApplicationId], *)(*))
        .thenReturn(successful(UpdateOverridesSuccessResult))
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("test.key", "test name", "test description"))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin, SuppressIvForAgents(Set("test.key")))))

      result shouldBe UpdateOverridesSuccessResult

      verify(mockProductionApplicationConnector).updateOverrides(eqTo(stdApp1.id),
        eqTo(UpdateOverridesRequest(Set(PersistLogin, SuppressIvForAgents(Set("test.key"))))))(*)
    }

    "fail when called with invalid scopes" in new Setup {
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("test.key", "test name", "test description"))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin, SuppressIvForAgents(Set("test.key", "invalid.key")))))

      result shouldBe UpdateOverridesFailureResult(Set(SuppressIvForAgents(Set("test.key", "invalid.key"))))

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }

    "fail when called for an app with Privileged access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(privilegedApp, Set(PersistLogin, SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }

    "fail when called for an app with ROPC access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(ropcApp, Set(PersistLogin, SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }
  }

  "updateScopes" should {
    "call the service to update the scopes for an app with Privileged access" in new Setup {
      ApplicationConnectorMock.Prod.UpdateScopes.succeeds()
      ApiScopeConnectorMock.Prod.FetchAll.returns(
        ApiScope("hello", "test name", "test description"),
        ApiScope("individual-benefits", "test name", "test description")
      )

      val result = await(underTest.updateScopes(privilegedApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(eqTo(privilegedApp.id),
        eqTo(UpdateScopesRequest(Set("hello", "individual-benefits"))))(*)
    }

    "call the service to update the scopes for an app with ROPC access" in new Setup {
      ApplicationConnectorMock.Prod.UpdateScopes.succeeds()
      ApiScopeConnectorMock.Prod.FetchAll.returns(
        ApiScope("hello", "test name", "test description"),
        ApiScope("individual-benefits", "test name", "test description")
      )

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(eqTo(ropcApp.id),
        eqTo(UpdateScopesRequest(Set("hello", "individual-benefits"))))(*)
    }

    "fail when called with invalid scopes" in new Setup {
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("hello", "test name", "test description"))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesInvalidScopesResult

      verify(mockProductionApplicationConnector, never).updateScopes(*[ApplicationId], *)(*)
    }

    "fail when called for an app with Standard access" in new Setup {
      val result = await(underTest.updateScopes(stdApp1, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesInvalidScopesResult

      verify(mockProductionApplicationConnector, never).updateScopes(*[ApplicationId], *)(*)
    }
  }

  "manageIpAllowlist" should {
    "send the updated IP allowlist to the TPA connector" in new Setup {
      val existingIpAllowlist = IpAllowlist(required = false, Set("192.168.1.0/24"))
      val app: ApplicationResponse = stdApp1.copy(ipAllowlist = existingIpAllowlist)
      val newIpAllowlist = IpAllowlist(required = true, Set("192.168.1.0/24", "192.168.2.0/24"))
      ApplicationConnectorMock.Prod.UpdateIpAllowlist.succeeds()

      val result: UpdateIpAllowlistResult = await(underTest.manageIpAllowlist(app, newIpAllowlist.required, newIpAllowlist.allowlist))

      result shouldBe UpdateIpAllowlistSuccessResult
      verify(mockProductionApplicationConnector).updateIpAllowlist(eqTo(app.id), eqTo(newIpAllowlist.required), eqTo(newIpAllowlist.allowlist))(*)
    }

    "propagate connector errors" in new Setup {
      ApplicationConnectorMock.Prod.UpdateIpAllowlist.failsWithISE()

      intercept[UpstreamErrorResponse] {
        await(underTest.manageIpAllowlist(stdApp1, false, Set("192.168.1.0/24")))
      }
    }
  }

  "subscribeToApi" should {
    "calls APM connector only now" in new Setup {
      ApmConnectorMock.SubscribeToApi.succeeds()

      val result = await(underTest.subscribeToApi(stdApp1, apiIdentifier))

      result shouldBe ApplicationUpdateSuccessResult
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API and delete the field values" in new Setup {

      ApplicationConnectorMock.Prod.UnsubscribeFromApi.succeeds()

      val result = await(underTest.unsubscribeFromApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).unsubscribeFromApi(eqTo(stdApp1.id), eqTo(context), eqTo(version))(*)
    }
  }

  "updateRateLimitTier" should {
    "call the service to update the rate limit tier" in new Setup {
      when(mockProductionApplicationConnector.updateRateLimitTier(*[ApplicationId], *)(*))
        .thenReturn(successful(ApplicationUpdateSuccessResult))


      val result = await(underTest.updateRateLimitTier(stdApp1, RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).updateRateLimitTier(eqTo(stdApp1.id), eqTo(RateLimitTier.GOLD))(*)
    }
  }

  "createPrivOrROPCApp" should {
    val admin = List(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random))
    val totpSecrets = Some(TotpSecrets("secret"))
    val appAccess = AppAccess(AccessType.PRIVILEGED, List.empty)

    val name = "New app"
    val appId = ApplicationId.random
    val clientId = ClientId("client ID")
    val description = "App description"

    "call the production connector to create a new app in production" in new Setup {
      val environment = Environment.PRODUCTION

      ApplicationConnectorMock.Prod.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockProductionApplicationConnector).createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(*)
      verify(mockSandboxApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }

    "call the sandbox connector to create a new app in sandbox" in new Setup {
      val environment = Environment.SANDBOX

      ApplicationConnectorMock.Sandbox.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockSandboxApplicationConnector).createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(*)
      verify(mockProductionApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }
  }

  "removeTeamMember" should {
    val requestingUser = "admin.email@example.com"
    val memberToRemove = "email@testuser.com"

    "remove a member from a standard app successfully in the correct environment" in new Setup {
      val application = stdApp1

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      ApplicationConnectorMock.Prod.RemoveCollaborator.succeedsFor(application.id, memberToRemove, requestingUser)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from a privileged app in the correct environment" in new Setup {
      val application = privilegedApp

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      ApplicationConnectorMock.Prod.RemoveCollaborator.succeedsFor(application.id, memberToRemove, requestingUser)
      
      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from an ROPC app in the correct environment" in new Setup {
      val application = ropcApp

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      ApplicationConnectorMock.Prod.RemoveCollaborator.succeedsFor(application.id, memberToRemove, requestingUser)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "propagate TeamMemberLastAdmin error from application connector" in new Setup {

      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(List.empty))
      ApplicationConnectorMock.Prod.RemoveCollaborator.failsWithLastAdminFor(stdApp1.id, memberToRemove, requestingUser)

      intercept[TeamMemberLastAdmin.type] {
        await(underTest.removeTeamMember(stdApp1, memberToRemove, requestingUser))
      }
    }

    "include correct set of admins to email" in new Setup {
      val verifiedAdmin = Collaborator("verified@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random)
      val unverifiedAdmin = Collaborator("unverified@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random)
      val adminToRemove = Collaborator(memberToRemove, CollaboratorRole.ADMINISTRATOR, UserId.random)
      val adderAdmin = Collaborator(requestingUser, CollaboratorRole.ADMINISTRATOR, UserId.random)
      val verifiedDeveloper = Collaborator("developer@example.com", CollaboratorRole.DEVELOPER, UserId.random)
      val application = stdApp1.copy(collaborators = Set(verifiedAdmin, unverifiedAdmin, adminToRemove, adderAdmin, verifiedDeveloper))
      val nonAdderAdmins = List(
        RegisteredUser(verifiedAdmin.emailAddress, UserId.random, "verified", "user", true),
        RegisteredUser(unverifiedAdmin.emailAddress, UserId.random, "unverified", "user", false)
      )
      val response = ApplicationUpdateSuccessResult
      val expectedAdminsToEmail = Set(verifiedAdmin.emailAddress)

      when(mockDeveloperConnector.fetchByEmails(eqTo(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(*))
        .thenReturn(successful(nonAdderAdmins))
        ApplicationConnectorMock.Prod.RemoveCollaborator.succeeds()

      await(underTest.removeTeamMember(application, memberToRemove, adderAdmin.emailAddress)) shouldBe response

      verify(mockProductionApplicationConnector)
        .removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), eqTo(expectedAdminsToEmail))(*)
    }
  }

  "approveUplift" should {
    "approve the uplift in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")

      when(mockProductionApplicationConnector.approveUplift(*[ApplicationId], *)(*))
        .thenReturn(successful(ApproveUpliftSuccessful))

      val result = await(underTest.approveUplift(application, gatekeeperUserId))

      result shouldBe ApproveUpliftSuccessful

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).approveUplift(eqTo(application.id), eqTo(gatekeeperUserId))(*)
    }
  }

  "rejectUplift" should {
    "reject the uplift in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val rejectionReason = "Rejected"

      when(mockSandboxApplicationConnector.rejectUplift(*[ApplicationId], *, *)(*))
        .thenReturn(successful(RejectUpliftSuccessful))

      val result = await(underTest.rejectUplift(application, gatekeeperUserId, rejectionReason))

      result shouldBe RejectUpliftSuccessful

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).rejectUplift(eqTo(application.id), eqTo(gatekeeperUserId), eqTo(rejectionReason))(*)
    }
  }

  "deleteApplication" should {
    "delete the application in the correct environment" in new Setup {
      val emailAddress = "email@example.com"
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val deleteApplicationRequest = DeleteApplicationRequest(gatekeeperUserId, emailAddress)

      ApplicationConnectorMock.Prod.DeleteApplication.succeeds()

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationDeleteSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).deleteApplication(eqTo(application.id), eqTo(deleteApplicationRequest))(*)
    }

    "propagate ApplicationDeleteFailureResult from connector" in new Setup {
      val emailAddress = "email@example.com"
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val deleteApplicationRequest = DeleteApplicationRequest(gatekeeperUserId, emailAddress)

      ApplicationConnectorMock.Sandbox.DeleteApplication.fails()

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationDeleteFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).deleteApplication(eqTo(application.id), eqTo(deleteApplicationRequest))(*)
    }
  }

  "blockApplication" should {
    "block the application in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val blockApplicationRequest = BlockApplicationRequest(gatekeeperUserId)

      ApplicationConnectorMock.Prod.BlockApplication.succeeds()

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).blockApplication(eqTo(application.id), eqTo(blockApplicationRequest))(*)
      verify(mockSandboxApplicationConnector, never).blockApplication(*[ApplicationId], *)(*)
    }

    "propagate ApplicationBlockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val blockApplicationRequest = BlockApplicationRequest(gatekeeperUserId)

      ApplicationConnectorMock.Sandbox.BlockApplication.fails()

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).blockApplication(eqTo(application.id), eqTo(blockApplicationRequest))(*)
      verify(mockProductionApplicationConnector, never).blockApplication(*[ApplicationId], *)(*)
    }
  }

  "unblockApplication" should {
    "unblock the application in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val unblockApplicationRequest = UnblockApplicationRequest(gatekeeperUserId)

      ApplicationConnectorMock.Prod.UnblockApplication.succeeds()

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).unblockApplication(eqTo(application.id), eqTo(unblockApplicationRequest))(*)
      verify(mockSandboxApplicationConnector, times(0)).unblockApplication(*[ApplicationId], *)(*)
    }

    "propagate ApplicationUnblockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val unblockApplicationRequest = UnblockApplicationRequest(gatekeeperUserId)

      ApplicationConnectorMock.Sandbox.UnblockApplication.fails()

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).unblockApplication(eqTo(application.id), eqTo(unblockApplicationRequest))(*)
      verify(mockProductionApplicationConnector, never).unblockApplication(*[ApplicationId], *)(*)
    }
  }

  "applicationConnectorFor" should {
    "return the production application connector for an application deployed to production" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockProductionApplicationConnector
    }

    "return the sandbox application connector for an application deployed to sandbox" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockSandboxApplicationConnector
    }
  }

  "apiScopeConnectorFor" should {
    "return the production api scope connector for an application deployed to production" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")

      val result = underTest.apiScopeConnectorFor(application)

      result shouldBe mockProductionApiScopeConnector
    }

    "return the sandbox api scope connector for an application deployed to sandbox" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")

      val result = underTest.apiScopeConnectorFor(application)

      result shouldBe mockSandboxApiScopeConnector
    }
  }
}
