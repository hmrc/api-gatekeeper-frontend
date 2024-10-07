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

package uk.gov.hmrc.gatekeeper.services

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import mocks.connectors.{ApmConnectorMockProvider, ApplicationConnectorMockProvider, CommandConnectorMockProvider}
import mocks.services.ApiScopeConnectorMockProvider
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._

class ApplicationServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest with ApplicationWithCollaboratorsFixtures with ApiIdentifierFixtures {

  trait Setup
      extends MockitoSugar with ArgumentMatchersSugar
      with ApplicationConnectorMockProvider
      with ApmConnectorMockProvider
      with CommandConnectorMockProvider
      with ApiScopeConnectorMockProvider
      with ApplicationBuilder {
    val mockDeveloperConnector        = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val applicationService = new ApplicationService(
      mockSandboxApplicationConnector,
      mockProductionApplicationConnector,
      mockSandboxApiScopeConnector,
      mockProductionApiScopeConnector,
      ApmConnectorMock.aMock,
      mockDeveloperConnector,
      mockSubscriptionFieldsService,
      CommandConnectorMock.aMock,
      FixedClock.clock
    )
    val underTest          = spy(applicationService)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val collaborators = someCollaborators

    val applicationWithHistory = ApplicationWithHistory(standardApp, List.empty)
    val gatekeeperUserId       = "loggedin.gatekeeper"
    val gatekeeperUser         = Actors.GatekeeperUser("Bob Smith")

    val apiIdentifier = ApiIdentifier(ApiContext.random, ApiVersionNbr.random)

    val context = apiIdentifier.context
    val version = apiIdentifier.versionNbr

    val allProductionApplications = List(standardApp, privilegedApp, standardApp3)
    val allSandboxApplications    = allProductionApplications.map(_.withEnvironment(Environment.SANDBOX))

    def updateGrantLengthCommandWillSucceed = {
      CommandConnectorMock.IssueCommand.succeeds()
    }
  }

  trait SubscriptionFieldsServiceSetup extends Setup {
    def subscriptionFields: List[SubscriptionFieldValue]
  }

  "searchApplications" should {
    "list all subscribed applications from production when PRODUCTION environment is specified" in new Setup {
      ApplicationConnectorMock.Prod.SearchApplications.returns(allProductionApplications: _*)

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(Environment.PRODUCTION), Map.empty))

      val app1 = result.applications.find(sa => sa.name == standardApp.name).value
      val app2 = result.applications.find(sa => sa.name == privilegedApp.name).value
      val app3 = result.applications.find(sa => sa.name == standardApp3.name).value

      app1 shouldBe standardApp
      app2 shouldBe privilegedApp
      app3 shouldBe standardApp3
    }

    "list all subscribed applications from sandbox when SANDBOX environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.SearchApplications.returns(allSandboxApplications: _*)
      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(Environment.SANDBOX), Map.empty))

      val app1 = result.applications.find(sa => sa.name == standardApp.name).value
      val app2 = result.applications.find(sa => sa.name == privilegedApp.name).value
      val app3 = result.applications.find(sa => sa.name == standardApp3.name).value

      app1 shouldBe standardApp.withEnvironment(Environment.SANDBOX)
      app2 shouldBe privilegedApp.withEnvironment(Environment.SANDBOX)
      app3 shouldBe standardApp3.withEnvironment(Environment.SANDBOX)
    }

    "list all subscribed applications from sandbox when no environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.SearchApplications.returns(allSandboxApplications: _*)

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(None, Map.empty))

      val app1 = result.applications.find(sa => sa.name == appNameOne).get
      val app2 = result.applications.find(sa => sa.name == appNameTwo).get
      val app3 = result.applications.find(sa => sa.name == appNameThree).get

      app1.isSandbox shouldBe true
      app2.isSandbox shouldBe true
      app3.isSandbox shouldBe true
    }
  }

  "fetchApplicationsWithSubscriptions" should {
    val resp1 = standardApp
      .withName(appNameOne)
      .withSubscriptions(
        Set(
          apiIdentifierOne,
          apiIdentifierTwo,
          apiIdentifierThree
        )
      )
    val resp2 = standardApp
      .withName(appNameTwo)
      .withSubscriptions(
        Set(
          apiIdentifierOne,
          apiIdentifierTwo
        )
      )
    val resp3 = standardApp
      .withName(appNameThree)
      .withSubscriptions(
        Set()
      )

    "list all applications from production when PRODUCTION environment is specified" in new Setup {
      ApplicationConnectorMock.Prod.FetchApplicationsWithSubscriptions.returns(resp1, resp2)

      val result: List[ApplicationWithSubscriptions] = await(underTest.fetchApplicationsWithSubscriptions(Some(Environment.PRODUCTION)))

      val app1 = result.find(sa => sa.name == appNameOne).get
      val app2 = result.find(sa => sa.name == appNameTwo).get

      app1 shouldBe resp1
      app2 shouldBe resp2
    }

    "list all applications from sandbox when SANDBOX environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.FetchApplicationsWithSubscriptions.returns(resp3)

      val result = await(underTest.fetchApplicationsWithSubscriptions(Some(Environment.SANDBOX)))

      result.head shouldBe resp3
    }

    "list all applications from sandbox when no environment is specified" in new Setup {
      ApplicationConnectorMock.Sandbox.FetchApplicationsWithSubscriptions.returns(resp3)

      val result = await(underTest.fetchApplicationsWithSubscriptions(None))

      result.head shouldBe resp3
    }
  }

  "resendVerification" should {
    "call commandConnector with appropriate parameters" in new Setup {
      val userName = "userName"

      CommandConnectorMock.IssueCommand.succeeds()

      await(underTest.resendVerification(standardApp, userName))

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.ResendRequesterEmailVerification(aUser, _) =>
          aUser shouldBe userName
      }
    }
  }

  "fetchProdAppStateHistories" should {
    def buildAppStateHistories(states: State*) =
      ApplicationStateHistory(ApplicationId.random, "app name", 2, states.toList.map(ApplicationStateHistoryItem(_, LocalDateTime.now)))

    "handle apps with no state history correctly" in new Setup {
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(
        buildAppStateHistories()
      )
      val result = await(underTest.fetchProdAppStateHistories())
      result shouldEqual List()
    }

    "handle apps with single state history item correctly" in new Setup {
      val appStateHistory = buildAppStateHistories(State.TESTING)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(appStateHistory)

      val result = await(underTest.fetchProdAppStateHistories())

      result shouldEqual List(
        ApplicationStateHistoryChange(
          appStateHistory.applicationId.value.toString(),
          appStateHistory.appName,
          appStateHistory.journeyVersion.toString,
          "TESTING",
          appStateHistory.stateHistory(0).timestamp.toString,
          "",
          ""
        )
      )
    }

    "handle apps with multiple state history items correctly" in new Setup {
      val appStateHistory = buildAppStateHistories(State.TESTING, State.PENDING_GATEKEEPER_APPROVAL, State.PRODUCTION)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(appStateHistory)

      val result = await(underTest.fetchProdAppStateHistories())

      result shouldEqual List(
        ApplicationStateHistoryChange(
          appStateHistory.applicationId.value.toString(),
          appStateHistory.appName,
          appStateHistory.journeyVersion.toString,
          "TESTING",
          appStateHistory.stateHistory(0).timestamp.toString,
          "PENDING_GATEKEEPER_APPROVAL",
          appStateHistory.stateHistory(1).timestamp.toString
        ),
        ApplicationStateHistoryChange(
          appStateHistory.applicationId.value.toString(),
          appStateHistory.appName,
          appStateHistory.journeyVersion.toString,
          "PENDING_GATEKEEPER_APPROVAL",
          appStateHistory.stateHistory(1).timestamp.toString,
          Environment.PRODUCTION.toString,
          appStateHistory.stateHistory(2).timestamp.toString
        ),
        ApplicationStateHistoryChange(
          appStateHistory.applicationId.value.toString(),
          appStateHistory.appName,
          appStateHistory.journeyVersion.toString,
          Environment.PRODUCTION.toString,
          appStateHistory.stateHistory(2).timestamp.toString,
          "",
          ""
        )
      )
    }
    "handle multiple apps in response correctly" in new Setup {
      val app1StateHistory = buildAppStateHistories(State.TESTING)
      val app2StateHistory = buildAppStateHistories(State.TESTING, State.PRODUCTION)
      ApplicationConnectorMock.Prod.FetchAllApplicationsWithStateHistories.returns(app1StateHistory, app2StateHistory)

      val result = await(underTest.fetchProdAppStateHistories())

      result shouldEqual List(
        ApplicationStateHistoryChange(
          app1StateHistory.applicationId.value.toString(),
          app1StateHistory.appName,
          app1StateHistory.journeyVersion.toString,
          "TESTING",
          app1StateHistory.stateHistory(0).timestamp.toString,
          "",
          ""
        ),
        ApplicationStateHistoryChange(
          app2StateHistory.applicationId.value.toString(),
          app2StateHistory.appName,
          app2StateHistory.journeyVersion.toString,
          "TESTING",
          app2StateHistory.stateHistory(0).timestamp.toString,
          Environment.PRODUCTION.toString,
          app2StateHistory.stateHistory(1).timestamp.toString
        ),
        ApplicationStateHistoryChange(
          app2StateHistory.applicationId.value.toString(),
          app2StateHistory.appName,
          app2StateHistory.journeyVersion.toString,
          Environment.PRODUCTION.toString,
          app2StateHistory.stateHistory(1).timestamp.toString,
          "",
          ""
        )
      )
    }
  }

  "fetchApplications" should {
    "list all applications from sandbox and production when filtering not provided" in new Setup {
      ApplicationConnectorMock.Prod.FetchAllApplications.returns(allProductionApplications: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplications.returns(allProductionApplications: _*)

      val result: Seq[ApplicationWithCollaborators] = await(underTest.fetchApplications)
      result shouldEqual allProductionApplications

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list filtered applications from sandbox and production when specific subscription filtering is provided" in new Setup {
      val filteredApplications = List(standardApp, privilegedApp)

      ApplicationConnectorMock.Prod.FetchAllApplicationsBySubscription.returns(filteredApplications: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplicationsBySubscription.returns(filteredApplications: _*)

      val result = await(underTest.fetchApplications(Value("subscription", "version"), AnyEnvironment))
      result shouldBe filteredApplications

      verify(mockProductionApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
    }

    "list filtered applications from sandbox and production when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(standardApp, privilegedApp)
      val subscriptions   = List(standardApp2, ropcApp)

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
      val allApps = List(standardApp, privilegedApp)

      ApplicationConnectorMock.Prod.FetchAllApplications.returns(allApps: _*)
      ApplicationConnectorMock.Sandbox.FetchAllApplications.returns()

      val result = await(underTest.fetchApplications(OneOrMoreApplications, AnyEnvironment))
      result shouldBe allApps

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list distinct filtered applications from sandbox and production when NoSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(standardApp, privilegedApp)

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

      val result = await(underTest.fetchApplication(standardApp.id))

      result shouldBe applicationWithHistory

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(standardApp.id))(*)
      verify(mockSandboxApplicationConnector, never).fetchApplication(*[ApplicationId])(*)
    }

    "return the the app in sandbox when not found in production" in new Setup {
      ApplicationConnectorMock.Prod.FetchApplication.failsNotFound()
      ApplicationConnectorMock.Sandbox.FetchApplication.returns(applicationWithHistory)

      await(underTest.fetchApplication(standardApp.id))

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(standardApp.id))(*)
      verify(mockSandboxApplicationConnector).fetchApplication(eqTo(standardApp.id))(*)
    }
  }

  "UpdateOverrides" should {
    "call the service to update the overrides for an app with Standard access" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("test.key", "test name", "test description"))

      val validOverrides: Set[OverrideFlag] = Set(OverrideFlag.PersistLogin, OverrideFlag.SuppressIvForAgents(Set("test.key")))
      val result                            = await(underTest.updateOverrides(standardApp, validOverrides, gatekeeperUserId))

      result shouldBe UpdateOverridesSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.ChangeApplicationAccessOverrides(aUser, overrides, _) =>
          aUser shouldBe gatekeeperUserId
          overrides shouldBe validOverrides
      }
    }

    "fail when called with invalid overrides" in new Setup {
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("test.key", "test name", "test description"))

      val invalidOverrides: Set[OverrideFlag] = Set(OverrideFlag.PersistLogin, OverrideFlag.SuppressIvForAgents(Set("test.key", "invalid.key")))
      val result                              = await(underTest.updateOverrides(standardApp, invalidOverrides, gatekeeperUserId))

      result shouldBe UpdateOverridesFailureResult(Set(OverrideFlag.SuppressIvForAgents(Set("test.key", "invalid.key"))))

      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }

    "fail when called for an app with Privileged access" in new Setup {
      val result = await(underTest.updateOverrides(privilegedApp, Set(OverrideFlag.PersistLogin, OverrideFlag.SuppressIvForAgents(Set("hello"))), gatekeeperUserId))

      result shouldBe UpdateOverridesFailureResult()

      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }

    "fail when called for an app with ROPC access" in new Setup {
      val result = await(underTest.updateOverrides(ropcApp, Set(OverrideFlag.PersistLogin, OverrideFlag.SuppressIvForAgents(Set("hello"))), gatekeeperUserId))

      result shouldBe UpdateOverridesFailureResult()

      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }
  }

  "UpdateScopes" should {
    "call the service to update the scopes for an app with Privileged access" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()
      ApiScopeConnectorMock.Prod.FetchAll.returns(
        ApiScope("hello", "test name", "test description"),
        ApiScope("individual-benefits", "test name", "test description")
      )

      val result = await(underTest.updateScopes(privilegedApp, Set("hello", "individual-benefits"), gatekeeperUserId))

      result shouldBe UpdateScopesSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(privilegedApp.id)) {
        case ApplicationCommands.ChangeApplicationScopes(aUser, scopes, _) =>
          aUser shouldBe gatekeeperUserId
          scopes shouldBe Set("hello", "individual-benefits")
      }
    }

    "call the service to update the scopes for an app with ROPC access" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()
      ApiScopeConnectorMock.Prod.FetchAll.returns(
        ApiScope("hello", "test name", "test description"),
        ApiScope("individual-benefits", "test name", "test description")
      )

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits"), gatekeeperUserId))

      result shouldBe UpdateScopesSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(ropcApp.id)) {
        case ApplicationCommands.ChangeApplicationScopes(aUser, scopes, _) =>
          aUser shouldBe gatekeeperUserId
          scopes shouldBe Set("hello", "individual-benefits")
      }
    }

    "fail when called with invalid scopes" in new Setup {
      CommandConnectorMock.IssueCommand.failsWith(CommandFailures.GenericFailure("Bang"))
      ApiScopeConnectorMock.Prod.FetchAll.returns(ApiScope("hello", "test name", "test description"))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits"), gatekeeperUserId))

      result shouldBe UpdateScopesInvalidScopesResult

      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }

    "fail when called for an app with Standard access" in new Setup {
      val result = await(underTest.updateScopes(standardApp, Set("hello", "individual-benefits"), gatekeeperUserId))

      result shouldBe UpdateScopesInvalidScopesResult

      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }
  }

  "manageIpAllowlist" should {
    "issue the command to update the ip allowlist" in new Setup {
      val existingIpAllowlist               = IpAllowlist(required = false, Set("192.168.1.0/24"))
      val app: ApplicationWithCollaborators = standardApp.modify(_.copy(ipAllowlist = existingIpAllowlist))
      val newIpAllowlist                    = IpAllowlist(required = true, Set("192.168.1.0/24", "192.168.2.0/24"))
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.manageIpAllowlist(app, newIpAllowlist.required, newIpAllowlist.allowlist, gatekeeperUserId))

      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.ChangeIpAllowlist(aUser, _, required, oldIpAllowlist, newIpAllowlist) =>
          aUser shouldBe Actors.GatekeeperUser(gatekeeperUserId)
          required shouldBe true
          oldIpAllowlist shouldBe List(CidrBlock("192.168.1.0/24"))
          newIpAllowlist shouldBe List(CidrBlock("192.168.1.0/24"), CidrBlock("192.168.2.0/24"))
      }
    }
  }

  "updateRateLimitTier" should {
    "call the service to update the rate limit tier" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.updateRateLimitTier(standardApp, RateLimitTier.GOLD, gatekeeperUserId))

      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.ChangeRateLimitTier(aUser, _, rateLimitTier) =>
          aUser shouldBe gatekeeperUserId
          rateLimitTier shouldBe RateLimitTier.GOLD
      }
    }
  }

  "createPrivOrROPCApp" should {
    val admin       = List(Collaborators.Administrator(UserId.random, "admin@example.com".toLaxEmail))
    val totpSecrets = Some(TotpSecrets("secret"))
    val appAccess   = AppAccess(AccessType.PRIVILEGED, List.empty)

    val name        = "New app"
    val appId       = ApplicationId.random
    val clientId    = ClientId("client ID")
    val description = "App description"

    "call the production connector to create a new app in production" in new Setup {
      val environment = Environment.PRODUCTION

      ApplicationConnectorMock.Prod.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess)

      verify(mockProductionApplicationConnector).createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment, name, description, admin, appAccess)))(*)
      verify(mockSandboxApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }

    "call the sandbox connector to create a new app in sandbox" in new Setup {
      val environment = Environment.SANDBOX

      ApplicationConnectorMock.Sandbox.CreatePrivOrROPCApp.returns(CreatePrivOrROPCAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess)

      verify(mockSandboxApplicationConnector).createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment, name, description, admin, appAccess)))(*)
      verify(mockProductionApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }
  }

  "deleteApplication" should {
    "delete the application in the correct environment" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val emailAddress = "email@example.com".toLaxEmail
      val application  = standardApp.withEnvironment(Environment.PRODUCTION)
      val reasons      = "Application deleted by Gatekeeper user"

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.DeleteApplicationByGatekeeper(aUser, aRequestor, aReason, _) =>
          aUser shouldBe gatekeeperUserId
          aReason shouldBe reasons
          aRequestor shouldBe emailAddress
      }
    }

  }

  "BlockApplication" should {
    "block the application" in new Setup {
      val application = standardApp.withEnvironment(Environment.PRODUCTION)

      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(application.id)) {
        case ApplicationCommands.BlockApplication(aUser, _) =>
          aUser shouldBe gatekeeperUserId
      }
    }

    "propagate failure from connector" in new Setup {
      val application = standardApp.withEnvironment(Environment.SANDBOX)

      CommandConnectorMock.IssueCommand.failsWith(CommandFailures.GenericFailure("Bang"))

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockFailureResult
    }
  }

  "UnblockApplication" should {
    "unblock the application in the correct environment" in new Setup {
      val application = standardApp.withEnvironment(Environment.PRODUCTION)

      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(application.id)) {
        case ApplicationCommands.UnblockApplication(aUser, _) =>
          aUser shouldBe gatekeeperUserId
      }
    }

    "propagate failure from connector" in new Setup {
      val application = standardApp.withEnvironment(Environment.SANDBOX)

      CommandConnectorMock.IssueCommand.failsWith(CommandFailures.GenericFailure("Bang"))

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockFailureResult
    }
  }

  "applicationConnectorFor" should {
    "return the production application connector for an application deployed to production" in new Setup {
      val application = standardApp.withEnvironment(Environment.PRODUCTION)

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockProductionApplicationConnector
    }

    "return the sandbox application connector for an application deployed to sandbox" in new Setup {
      val application = standardApp.withEnvironment(Environment.SANDBOX)

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockSandboxApplicationConnector
    }
  }

  "apiScopeConnectorFor" should {
    "return the production api scope connector for an application deployed to production" in new Setup {
      val application = standardApp.withEnvironment(Environment.PRODUCTION)

      val result = underTest.apiScopeConnectorFor(application)

      result shouldBe mockProductionApiScopeConnector
    }

    "return the sandbox api scope connector for an application deployed to sandbox" in new Setup {
      val application = standardApp.withEnvironment(Environment.SANDBOX)

      val result = underTest.apiScopeConnectorFor(application)

      result shouldBe mockSandboxApiScopeConnector
    }
  }

  "updateGrantLength" should {
    "update grant length in either sandbox or production app" in new Setup {
      updateGrantLengthCommandWillSucceed

      val result = await(underTest.updateGrantLength(standardApp, GrantLength.THREE_MONTHS, gatekeeperUserId))
      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.ChangeGrantLength(aUser, _, length) =>
          aUser shouldBe gatekeeperUserId
          length shouldBe GrantLength.THREE_MONTHS
      }
    }
  }

  "updateAutoDelete" should {
    val noReason = "No reasons given"
    val reason   = "Some reason"

    "issue AllowApplicationAutoDelete command when auto delete is true in either sandbox or production app" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.updateAutoDelete(standardApp.id, true, gatekeeperUserId, noReason))
      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.AllowApplicationAutoDelete(aUser, aReason, _) =>
          aUser shouldBe gatekeeperUserId
          aReason shouldBe noReason
      }
    }

    "issue BlockApplicationAutoDelete command when auto delete is false in either sandbox or production app" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.updateAutoDelete(standardApp.id, false, gatekeeperUserId, reason))
      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.BlockApplicationAutoDelete(aUser, aReason, _) =>
          aUser shouldBe gatekeeperUserId
          aReason shouldBe reason
      }
    }
  }
}
