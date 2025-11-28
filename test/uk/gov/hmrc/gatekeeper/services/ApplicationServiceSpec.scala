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

import scala.concurrent.ExecutionContext.Implicits.global

import mocks.connectors.{ApplicationConnectorMockProvider, CommandConnectorMockProvider}
import mocks.services.ApiScopeConnectorMockProvider
import org.mockito.scalatest.ResetMocksAfterEachTest
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.{CreateApplicationRequestV1, CreationAccess}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.mocks.connectors.ThirdPartyOrchestratorConnectorMockProvider
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationsByAnswer

class ApplicationServiceSpec extends AsyncHmrcSpec with ResetMocksAfterEachTest with ApplicationWithCollaboratorsFixtures with ApiIdentifierFixtures {

  trait Setup
      extends MockitoSugar with ArgumentMatchersSugar
      with ApplicationConnectorMockProvider
      with ThirdPartyOrchestratorConnectorMockProvider
      with CommandConnectorMockProvider
      with ApiScopeConnectorMockProvider
      with ApplicationBuilder {

    val applicationService = new ApplicationService(
      mockSandboxApplicationConnector,
      mockProductionApplicationConnector,
      mockSandboxApiScopeConnector,
      mockProductionApiScopeConnector,
      TPOConnectorMock.aMock,
      CommandConnectorMock.aMock,
      FixedClock.clock
    )
    val underTest          = spy(applicationService)

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val collaborators = someCollaborators

    val gatekeeperUserId = "loggedin.gatekeeper"
    val gatekeeperUser   = Actors.GatekeeperUser("Bob Smith")

    val apiIdentifier = ApiIdentifier(ApiContext.random, ApiVersionNbr.random)

    val context = apiIdentifier.context
    val version = apiIdentifier.versionNbr

    val allProductionApplications = List(standardApp, privilegedApp, standardApp3)
    val allSandboxApplications    = allProductionApplications.map(_.inSandbox())

    def updateGrantLengthCommandWillSucceed = {
      CommandConnectorMock.IssueCommand.succeeds()
    }
  }

  trait SubscriptionFieldsServiceSetup extends Setup {
    def subscriptionFields: List[SubscriptionFieldValue]
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

  "createPrivApp" should {
    val admin       = List(Collaborators.Administrator(UserId.random, "admin@example.com".toLaxEmail))
    val totpSecrets = Some(TotpSecrets("secret"))
    val appAccess   = AppAccess(AccessType.PRIVILEGED, List.empty)

    val name        = ApplicationName("New app")
    val appId       = ApplicationId.random
    val clientId    = ClientId("client ID")
    val description = "App description"

    "call the production connector to create a new app in production" in new Setup {
      val environment = Environment.PRODUCTION

      ApplicationConnectorMock.Prod.CreatePrivApp.returns(CreatePrivAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivApp(environment, name.value, description, admin))

      result shouldBe CreatePrivAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess)

      verify(mockProductionApplicationConnector).createPrivApp(eqTo(CreateApplicationRequestV1(name, CreationAccess.Privileged, Some(description), environment, admin.toSet, None)))(*)
      verify(mockSandboxApplicationConnector, never).createPrivApp(*)(*)
    }

    "call the sandbox connector to create a new app in sandbox" in new Setup {
      val environment = Environment.SANDBOX

      ApplicationConnectorMock.Sandbox.CreatePrivApp.returns(CreatePrivAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess))

      val result = await(underTest.createPrivApp(environment, name.value, description, admin))

      result shouldBe CreatePrivAppSuccessResult(appId, name, environment, clientId, totpSecrets, appAccess)

      verify(mockSandboxApplicationConnector).createPrivApp(eqTo(CreateApplicationRequestV1(name, CreationAccess.Privileged, Some(description), environment, admin.toSet, None)))(*)
      verify(mockProductionApplicationConnector, never).createPrivApp(*)(*)
    }
  }

  "deleteApplication" should {
    "delete the application in the correct environment" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val emailAddress = "email@example.com".toLaxEmail
      val application  = standardApp
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
      val application = standardApp

      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(application.id)) {
        case ApplicationCommands.BlockApplication(aUser, _) =>
          aUser shouldBe gatekeeperUserId
      }
    }

    "propagate failure from connector" in new Setup {
      val application = standardApp.inSandbox()

      CommandConnectorMock.IssueCommand.failsWith(CommandFailures.GenericFailure("Bang"))

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockFailureResult
    }
  }

  "UnblockApplication" should {
    "unblock the application in the correct environment" in new Setup {
      val application = standardApp

      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(application.id)) {
        case ApplicationCommands.UnblockApplication(aUser, _) =>
          aUser shouldBe gatekeeperUserId
      }
    }

    "propagate failure from connector" in new Setup {
      val application = standardApp.inSandbox()

      CommandConnectorMock.IssueCommand.failsWith(CommandFailures.GenericFailure("Bang"))

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockFailureResult
    }
  }

  "applicationConnectorFor" should {
    "return the production application connector for an application deployed to production" in new Setup {
      val application = standardApp

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockProductionApplicationConnector
    }

    "return the sandbox application connector for an application deployed to sandbox" in new Setup {
      val application = standardApp.inSandbox()

      val result = underTest.applicationConnectorFor(application)

      result shouldBe mockSandboxApplicationConnector
    }
  }

  "apiScopeConnectorFor" should {
    "return the production api scope connector for an application deployed to production" in new Setup {
      val application = standardApp

      val result = underTest.apiScopeConnectorFor(application)

      result shouldBe mockProductionApiScopeConnector
    }

    "return the sandbox api scope connector for an application deployed to sandbox" in new Setup {
      val application = standardApp.inSandbox()

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
        case ApplicationCommands.ChangeGrantLength(aUser, _, aLength) =>
          aUser shouldBe gatekeeperUserId
          aLength shouldBe GrantLength.THREE_MONTHS
      }
    }
  }

  "updateDeleteRestriction" should {
    val noReason = "No reasons given"
    val reason   = "Some reason"

    "issue AllowApplicationAllowDelete command when allow delete is true in either sandbox or production app" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.updateDeleteRestriction(standardApp.id, true, gatekeeperUserId, noReason))
      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.AllowApplicationDelete(aUser, aReason, _) =>
          aUser shouldBe gatekeeperUserId
          aReason shouldBe noReason
      }
    }

    "issue RestrictApplicationDelete command when allow delete is false in either sandbox or production app" in new Setup {
      CommandConnectorMock.IssueCommand.succeeds()

      val result = await(underTest.updateDeleteRestriction(standardApp.id, false, gatekeeperUserId, reason))
      result shouldBe ApplicationUpdateSuccessResult

      inside(CommandConnectorMock.IssueCommand.verifyCommand(standardApp.id)) {
        case ApplicationCommands.RestrictApplicationDelete(aUser, aReason, _) =>
          aUser shouldBe gatekeeperUserId
          aReason shouldBe reason
      }
    }
  }
  "fetchApplicationsByAnswer" should {
    "fetch all from prod" in new Setup {
      private val appsByAnswer = List(ApplicationsByAnswer("12345", List(applicationIdOne)))
      ApplicationConnectorMock.Prod.FetchApplicationsByAnswer.returns(appsByAnswer)
      val result               = await(underTest.fetchApplicationsByAnswer("vat-registration-number"))
      result shouldBe appsByAnswer
    }
  }
}
