/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import connectors._
import model.Environment._
import model.SubscriptionFields._
import model._
import org.joda.time.DateTime
import org.mockito.captor.ArgCaptor
import org.mockito.BDDMockito._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.scalatest.ResetMocksAfterEachTest
import model.applications.NewApplication

class ApplicationServiceSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  trait Setup {
    val mockSandboxApplicationConnector = mock[SandboxApplicationConnector]
    val mockProductionApplicationConnector = mock[ProductionApplicationConnector]
    val mockSandboxApiScopeConnector = mock[SandboxApiScopeConnector]
    val mockProductionApiScopeConnector = mock[ProductionApiScopeConnector]
    val mockApmConnector = mock[ApmConnector]
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
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))

    val stdApp1 = ApplicationResponse(
      ApplicationId.random, ClientId("clientid1"), "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val stdApp2 = ApplicationResponse(
      ApplicationId.random, ClientId("clientid2"), "gatewayId2", "application2", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val privilegedApp = ApplicationResponse(
      ApplicationId.random, ClientId("clientid3"), "gatewayId3", "application3", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Privileged(), ApplicationState())
    val ropcApp = ApplicationResponse(
      ApplicationId.random, ClientId("clientid4"), "gatewayId4", "application4", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Ropc(), ApplicationState())
    val applicationWithHistory = ApplicationWithHistory(stdApp1, Seq.empty)
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
    val prefetchedDefinitions : DefinitionsByApiVersion = Map(apiIdentifier -> Seq(subscriptionFieldDefinition))
    val definitions = Seq(subscriptionFieldDefinition)
  }

  trait SubscriptionFieldsServiceSetup extends Setup {

    def subscriptionFields : Seq[SubscriptionFieldValue]

    given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
    given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiIdentifier, prefetchedDefinitions))
      .willReturn(subscriptionFields)
  }

  "searchApplications" should {

    "list all subscribed applications from production when PRODUCTION environment is specified" in new Setup {
      given(mockProductionApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allProductionApplications)))

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(PRODUCTION), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1 shouldBe stdApp1
      app2 shouldBe stdApp2
      app3 shouldBe privilegedApp
    }

    "list all subscribed applications from sandbox when SANDBOX environment is specified" in new Setup {
      given(mockSandboxApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

      val result: PaginatedApplicationResponse = await(underTest.searchApplications(Some(SANDBOX), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.deployedTo shouldBe "SANDBOX"
      app2.deployedTo shouldBe "SANDBOX"
      app3.deployedTo shouldBe "SANDBOX"
    }

    "list all subscribed applications from sandbox when no environment is specified" in new Setup {
      given(mockSandboxApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

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

      given(mockProductionApplicationConnector.resendVerification(appIdCaptor, gatekeeperIdCaptor)(*))
        .willReturn(Future.successful(ResendVerificationSuccessful))

      await(underTest.resendVerification(stdApp1, userName))
      gatekeeperIdCaptor.value shouldBe userName
      appIdCaptor.value shouldBe stdApp1.id
    }
  }

  "fetchApplications" should {

    "list all applications from sandbox and production when filtering not provided" in new Setup {
      given(mockProductionApplicationConnector.fetchAllApplications()(*))
        .willReturn(Future.successful(allProductionApplications))
      given(mockSandboxApplicationConnector.fetchAllApplications()(*))
        .willReturn(Future.successful(allProductionApplications))

      val result: Seq[ApplicationResponse] = await(underTest.fetchApplications)
      result shouldEqual allProductionApplications

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list filtered applications from sandbox and production when specific subscription filtering is provided" in new Setup {
      val filteredApplications = List(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplicationsBySubscription(*, *)(*))
        .willReturn(Future.successful(filteredApplications))
      given(mockSandboxApplicationConnector.fetchAllApplicationsBySubscription(*, *)(*))
        .willReturn(Future.successful(filteredApplications))

      val result = await(underTest.fetchApplications(Value("subscription", "version"), AnyEnvironment))
      result shouldBe filteredApplications

      verify(mockProductionApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsBySubscription(eqTo("subscription"), eqTo("version"))(*)
    }

    "list filtered applications from sandbox and production when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(stdApp1, privilegedApp)
      val subscriptions = List(stdApp2, ropcApp)

      val allApps = noSubscriptions ++ subscriptions
      given(mockProductionApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(allApps))
      given(mockProductionApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))
      given(mockSandboxApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))

      val result = await(underTest.fetchApplications(OneOrMoreSubscriptions, AnyEnvironment))
      result shouldBe subscriptions

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
    }

    "list filtered applications from sandbox and production when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = List(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(List.empty))

      val result = await(underTest.fetchApplications(OneOrMoreApplications, AnyEnvironment))
      result shouldBe allApps

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list distinct filtered applications from sandbox and production when NoSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = List(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))
      given(mockSandboxApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))

      val result = await(underTest.fetchApplications(NoSubscriptions, AnyEnvironment))
      result shouldBe noSubscriptions

      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
    }
  }

  // "fetchApplicationsByEmail" should {
  //   "return apps from both production and sandbox" in new Setup {
  //     val emailAddress = "email@example.com"
  //     val productionApps = List(stdApp1, privilegedApp)
  //     val sandboxApps = List(stdApp1.copy(deployedTo = "SANDBOX"), privilegedApp.copy(deployedTo = "SANDBOX"))

  //     given(mockProductionApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(productionApps))
  //     given(mockSandboxApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(sandboxApps))

  //     val result = await(underTest.fetchApplicationsByEmail(emailAddress))

  //     result shouldBe sandboxApps ++ productionApps
  //   }

  //   "return only distinct apps" in new Setup {
  //     val emailAddress = "email@example.com"
  //     val allApps = List(stdApp1, privilegedApp)

  //     given(mockProductionApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(allApps))
  //     given(mockSandboxApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(allApps))

  //     val result = await(underTest.fetchApplicationsByEmail(emailAddress))

  //     result shouldBe allApps
  //   }
  // }

  "fetchApplication" should {
    "return the app when found in production" in new Setup {
      given(mockProductionApplicationConnector.fetchApplication(*[ApplicationId])(*))
        .willReturn(Future.successful(applicationWithHistory))
      given(mockSandboxApplicationConnector.fetchApplication(*[ApplicationId])(*))
        .willReturn(Future.failed(new NotFoundException("Not Found")))

      val result = await(underTest.fetchApplication(stdApp1.id))

      result shouldBe applicationWithHistory

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
      verify(mockSandboxApplicationConnector, never).fetchApplication(*[ApplicationId])(*)
    }

    "return the the app in sandbox when not found in production" in new Setup {
      given(mockProductionApplicationConnector.fetchApplication(*[ApplicationId])(*))
        .willReturn(Future.failed(new NotFoundException("Not Found")))
      given(mockSandboxApplicationConnector.fetchApplication(*[ApplicationId])(*))
        .willReturn(Future.successful(applicationWithHistory))

      await(underTest.fetchApplication(stdApp1.id))

      verify(mockProductionApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
      verify(mockSandboxApplicationConnector).fetchApplication(eqTo(stdApp1.id))(*)
    }
  }

  "updateOverrides" should {
    "call the service to update the overrides for an app with Standard access" in new Setup {
      given(mockProductionApplicationConnector.updateOverrides(*[ApplicationId], *)(*))
        .willReturn(Future.successful(UpdateOverridesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(*))
        .willReturn(Future.successful(Seq(ApiScope("test.key", "test name", "test description"))))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin(), SuppressIvForAgents(Set("test.key")))))

      result shouldBe UpdateOverridesSuccessResult

      verify(mockProductionApplicationConnector).updateOverrides(eqTo(stdApp1.id),
        eqTo(UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("test.key"))))))(*)
    }

    "fail when called with invalid scopes" in new Setup {
      given(mockProductionApiScopeConnector.fetchAll()(*))
        .willReturn(Future.successful(Seq(ApiScope("test.key", "test name", "test description"))))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin(), SuppressIvForAgents(Set("test.key", "invalid.key")))))

      result shouldBe UpdateOverridesFailureResult(Set(SuppressIvForAgents(Set("test.key", "invalid.key"))))

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }

    "fail when called for an app with Privileged access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(privilegedApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }

    "fail when called for an app with ROPC access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(ropcApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(*[ApplicationId], *)(*)
    }
  }

  "updateScopes" should {
    "call the service to update the scopes for an app with Privileged access" in new Setup {
      given(mockProductionApplicationConnector.updateScopes(*[ApplicationId], *)(*))
        .willReturn(Future.successful(UpdateScopesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(*))
        .willReturn(Future.successful(Seq(
          ApiScope("hello", "test name", "test description"),
          ApiScope("individual-benefits", "test name", "test description"))))

      val result = await(underTest.updateScopes(privilegedApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(eqTo(privilegedApp.id),
        eqTo(UpdateScopesRequest(Set("hello", "individual-benefits"))))(*)
    }

    "call the service to update the scopes for an app with ROPC access" in new Setup {
      given(mockProductionApplicationConnector.updateScopes(*[ApplicationId], *)(*))
        .willReturn(Future.successful(UpdateScopesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(*))
        .willReturn(Future.successful(Seq(
          ApiScope("hello", "test name", "test description"),
          ApiScope("individual-benefits", "test name", "test description"))))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(eqTo(ropcApp.id),
        eqTo(UpdateScopesRequest(Set("hello", "individual-benefits"))))(*)
    }

    "fail when called with invalid scopes" in new Setup {
      given(mockProductionApiScopeConnector.fetchAll()(*))
        .willReturn(Future.successful(Seq(ApiScope("hello", "test name", "test description"))))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesInvalidScopesResult

      verify(mockProductionApplicationConnector, never).updateScopes(*[ApplicationId], *)(*)
    }

    "fail when called for an app with Standard access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateScopes(stdApp1, Set("hello", "individual-benefits")))
      }

      verify(mockProductionApplicationConnector, never).updateScopes(*[ApplicationId], *)(*)
    }
  }

  "manageIpAllowlist" should {
    "send the updated IP allowlist to the TPA connector" in new Setup {
      val existingIpAllowlist = IpAllowlist(required = false, Set("192.168.1.0/24"))
      val app: ApplicationResponse = stdApp1.copy(ipAllowlist = existingIpAllowlist)
      val newIpAllowlist = IpAllowlist(required = true, Set("192.168.1.0/24", "192.168.2.0/24"))
      given(mockProductionApplicationConnector.updateIpAllowlist(*[ApplicationId], *, *)(*))
        .willReturn(Future.successful(UpdateIpAllowlistSuccessResult))

      val result: UpdateIpAllowlistResult = await(underTest.manageIpAllowlist(app, newIpAllowlist.required, newIpAllowlist.allowlist))

      result shouldBe UpdateIpAllowlistSuccessResult
      verify(mockProductionApplicationConnector).updateIpAllowlist(eqTo(app.id), eqTo(newIpAllowlist.required), eqTo(newIpAllowlist.allowlist))(*)
    }

    "propagate connector errors" in new Setup {
      given(mockProductionApplicationConnector.updateIpAllowlist(*[ApplicationId], *, *)(*))
        .willReturn(Future.failed(Upstream5xxResponse("Error", 500, 500)))

      intercept[Upstream5xxResponse] {
        await(underTest.manageIpAllowlist(stdApp1, false, Set("192.168.1.0/24")))
      }
    }
  }

  "subscribeToApi" should {
    "field definitions with empty values will persist empty values" in new Setup {
      given(mockApmConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
          .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, FieldValue.empty))

      given(mockSubscriptionFieldsService.fetchFieldsValues(*, *, *)(*))
        .willReturn(Future.successful(subscriptionFieldValues))
      
      given(mockSubscriptionFieldsService.saveBlankFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockApmConnector).subscribeToApi(eqTo(stdApp1.id), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService).fetchFieldsValues(eqTo(stdApp1), eqTo(definitions), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService).saveBlankFieldValues(eqTo(stdApp1), eqTo(context), eqTo(version), eqTo(subscriptionFieldValues))(*)
    }

    "field definitions with non-empty values will not persist anything" in new Setup {
      given(mockApmConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
        .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, FieldValue.random))

      given(mockSubscriptionFieldsService.fetchFieldsValues(eqTo(stdApp1), eqTo(definitions), eqTo(apiIdentifier))(*))
        .willReturn(Future.successful(subscriptionFieldValues))

      given(mockSubscriptionFieldsService.saveBlankFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))

      val fields = subscriptionFieldValues.map(v => v.definition.name -> v.value).toMap[FieldName, FieldValue]

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockApmConnector).subscribeToApi(eqTo(stdApp1.id), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService, never).saveFieldValues(*[NewApplication], eqTo(context), eqTo(version), eqTo(fields))(*)
      verify(mockSubscriptionFieldsService).saveBlankFieldValues(eqTo(stdApp1), eqTo(context), eqTo(version), eqTo(subscriptionFieldValues))(*)
    }

    "with field definitions but fails to save subscription fields throws error" in new Setup {
      given(mockApmConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
          .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, FieldValue.empty))

      given(mockSubscriptionFieldsService.fetchFieldsValues(*, *, *)(*))
        .willReturn(Future.successful(subscriptionFieldValues))

      val errors = Map("fieldName" -> "failure reason")

      given(mockSubscriptionFieldsService.saveBlankFieldValues(*, *[ApiContext], *[ApiVersion], *)(*))
          .willReturn(Future.successful(SaveSubscriptionFieldsFailureResponse(errors)))

      private val exception = intercept[RuntimeException](
          await(underTest.subscribeToApi(stdApp1, context, version))
      )
        
      exception.getMessage should include("failure reason")
      exception.getMessage should include("Failed to save blank subscription field values")
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API and delete the field values" in new Setup {

      given(mockProductionApplicationConnector.unsubscribeFromApi(*[ApplicationId], *[ApiContext], *[ApiVersion])(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val result = await(underTest.unsubscribeFromApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).unsubscribeFromApi(eqTo(stdApp1.id), eqTo(context), eqTo(version))(*)
    }
  }

  "updateRateLimitTier" should {
    "call the service to update the rate limit tier" in new Setup {
      given(mockProductionApplicationConnector.updateRateLimitTier(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))


      val result = await(underTest.updateRateLimitTier(stdApp1, RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).updateRateLimitTier(eqTo(stdApp1.id), eqTo(RateLimitTier.GOLD))(*)
    }
  }

  "createPrivOrROPCApp" should {
    val admin = Seq(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR))
    val totpSecrets = Some(TotpSecrets("secret"))
    val appAccess = AppAccess(AccessType.PRIVILEGED, Seq())

    val name = "New app"
    val appId = ApplicationId.random
    val clientId = ClientId("client ID")
    val description = "App description"

    "call the production connector to create a new app in production" in new Setup {
      val environment = Environment.PRODUCTION

      given(mockProductionApplicationConnector.createPrivOrROPCApp(*)(*))
        .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)))


      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockProductionApplicationConnector)
        .createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(*)
      verify(mockSandboxApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }

    "call the sandbox connector to create a new app in sandbox" in new Setup {
      val environment = Environment.SANDBOX

      given(mockSandboxApplicationConnector.createPrivOrROPCApp(*)(*))
        .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)))


      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockSandboxApplicationConnector)
        .createPrivOrROPCApp(eqTo(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(*)
      verify(mockProductionApplicationConnector, never).createPrivOrROPCApp(*)(*)
    }
  }

  "removeTeamMember" should {
    val requestingUser = "admin.email@example.com"
    val memberToRemove = "email@testuser.com"

    "remove a member from a standard app successfully in the correct environment" in new Setup {
      val application = stdApp1
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from a privileged app in the correct environment" in new Setup {
      val application = privilegedApp
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from an ROPC app in the correct environment" in new Setup {
      val application = ropcApp
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), *)(*)
      verify(underTest).applicationConnectorFor(application)
    }

    "propagate TeamMemberLastAdmin error from application connector" in new Setup {

      given(mockDeveloperConnector.fetchByEmails(*)(*))
        .willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(eqTo(stdApp1.id), eqTo(memberToRemove), eqTo(requestingUser), eqTo(Seq.empty))(*))
        .willReturn(Future.failed(new TeamMemberLastAdmin))

      intercept[TeamMemberLastAdmin] {
        await(underTest.removeTeamMember(stdApp1, memberToRemove, requestingUser))
      }
    }

    "include correct set of admins to email" in new Setup {
      val verifiedAdmin = Collaborator("verified@example.com", CollaboratorRole.ADMINISTRATOR)
      val unverifiedAdmin = Collaborator("unverified@example.com", CollaboratorRole.ADMINISTRATOR)
      val adminToRemove = Collaborator(memberToRemove, CollaboratorRole.ADMINISTRATOR)
      val adderAdmin = Collaborator(requestingUser, CollaboratorRole.ADMINISTRATOR)
      val verifiedDeveloper = Collaborator("developer@example.com", CollaboratorRole.DEVELOPER)
      val application = stdApp1.copy(collaborators = Set(verifiedAdmin, unverifiedAdmin, adminToRemove, adderAdmin, verifiedDeveloper))
      val nonAdderAdmins = Seq(
        User(verifiedAdmin.emailAddress, "verified", "user", Some(true)),
        User(unverifiedAdmin.emailAddress, "unverified", "user", Some(false)))
      val response = ApplicationUpdateSuccessResult
      val expectedAdminsToEmail = Seq(verifiedAdmin.emailAddress)

      given(mockDeveloperConnector.fetchByEmails(eqTo(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(*))
        .willReturn(Future.successful(nonAdderAdmins))
      given(mockProductionApplicationConnector.removeCollaborator(*[ApplicationId], *, *, *)(*))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, adderAdmin.emailAddress)) shouldBe response

      verify(mockProductionApplicationConnector)
        .removeCollaborator(eqTo(application.id), eqTo(memberToRemove), eqTo(requestingUser), eqTo(expectedAdminsToEmail))(*)
    }
  }

  "approveUplift" should {
    "approve the uplift in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")

      given(mockProductionApplicationConnector.approveUplift(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApproveUpliftSuccessful))

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

      given(mockSandboxApplicationConnector.rejectUplift(*[ApplicationId], *, *)(*))
        .willReturn(Future.successful(RejectUpliftSuccessful))

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

      given(mockProductionApplicationConnector.deleteApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationDeleteSuccessResult))

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationDeleteSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).deleteApplication(eqTo(application.id), eqTo(deleteApplicationRequest))(*)
    }

    "propagate ApplicationDeleteFailureResult from connector" in new Setup {
      val emailAddress = "email@example.com"
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val deleteApplicationRequest = DeleteApplicationRequest(gatekeeperUserId, emailAddress)

      given(mockSandboxApplicationConnector.deleteApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationDeleteFailureResult))

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

      given(mockProductionApplicationConnector.blockApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationBlockSuccessResult))

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).blockApplication(eqTo(application.id), eqTo(blockApplicationRequest))(*)
      verify(mockSandboxApplicationConnector, never).blockApplication(*[ApplicationId], *)(*)
    }

    "propagate ApplicationBlockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val blockApplicationRequest = BlockApplicationRequest(gatekeeperUserId)

      given(mockSandboxApplicationConnector.blockApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationBlockFailureResult))

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

      given(mockProductionApplicationConnector.unblockApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUnblockSuccessResult))

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).unblockApplication(eqTo(application.id), eqTo(unblockApplicationRequest))(*)
      verify(mockSandboxApplicationConnector, times(0)).unblockApplication(*[ApplicationId], *)(*)
    }

    "propagate ApplicationUnblockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val unblockApplicationRequest = UnblockApplicationRequest(gatekeeperUserId)

      given(mockSandboxApplicationConnector.unblockApplication(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUnblockFailureResult))

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

  private def aPaginatedApplicationResponse(applications: Seq[ApplicationResponse]): PaginatedApplicationResponse = {
    val page = 1
    val pageSize = 10
    PaginatedApplicationResponse(applications, page, pageSize, total = applications.size, matching = applications.size)
  }
}
