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

package services

import java.util.UUID

import connectors._
import model.Environment._
import model.RateLimitTier.RateLimitTier
import model.SubscriptionFields._
import model._
import org.joda.time.DateTime
import org.mockito.captor.{ArgCaptor, Captor, ValCaptor}
import org.mockito.BDDMockito._
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Random
import org.mockito.scalatest.ResetMocksAfterEachTest

class ApplicationServiceSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar with ResetMocksAfterEachTest {

  trait Setup {
    val mockSandboxApplicationConnector = mock[SandboxApplicationConnector]
    val mockProductionApplicationConnector = mock[ProductionApplicationConnector]
    val mockSandboxApiScopeConnector = mock[SandboxApiScopeConnector]
    val mockProductionApiScopeConnector = mock[ProductionApiScopeConnector]
    val mockDeveloperConnector = mock[DeveloperConnector]
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val applicationService = new ApplicationService(
      mockSandboxApplicationConnector,
      mockProductionApplicationConnector,
      mockSandboxApiScopeConnector,
      mockProductionApiScopeConnector,
      mockDeveloperConnector,
      mockSubscriptionFieldsService)
    val underTest = spy(applicationService)

    implicit val hc = HeaderCarrier()

    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))

    val stdApp1 = ApplicationResponse(
      ApplicationId(UUID.randomUUID().toString()), "clientid1", "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val stdApp2 = ApplicationResponse(
      ApplicationId(UUID.randomUUID().toString()), "clientid2", "gatewayId2", "application2", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val privilegedApp = ApplicationResponse(
      ApplicationId(UUID.randomUUID().toString()), "clientid3", "gatewayId3", "application3", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Privileged(), ApplicationState())
    val ropcApp = ApplicationResponse(
      ApplicationId(UUID.randomUUID().toString()), "clientid4", "gatewayId4", "application4", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Ropc(), ApplicationState())
    val applicationWithHistory = ApplicationWithHistory(stdApp1, Seq.empty)
    val gatekeeperUserId = "loggedin.gatekeeper"

    val apiIdentifier = APIIdentifier("a-context","1.0")

    val context = apiIdentifier.context
    val version = apiIdentifier.version

    val allProductionApplications = Seq(stdApp1, stdApp2, privilegedApp)
    val allSandboxApplications = allProductionApplications.map(_.copy(id = ApplicationId(UUID.randomUUID().toString()), deployedTo = "SANDBOX"))
  }

  trait SubscriptionFieldsServiceSetup  extends Setup {
    val prefetchedDefinitions : DefinitionsByApiVersion = Map(apiIdentifier -> Seq(SubscriptionFieldDefinition("name", "description", "hint", "String", "shortDescription")))

    def subscriptionFields : Seq[SubscriptionFieldValue]

    given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
    given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiIdentifier, prefetchedDefinitions))
      .willReturn(subscriptionFields)
  }

  "searchApplications" should {

    "list all subscribed applications from production when PRODUCTION environment is specified" in new Setup {
      given(mockProductionApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allProductionApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("test-context", "1.0"), Seq(allProductionApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("super-context", "1.0"), allProductionApplications.map(_.id.toString)))

      given(mockProductionApplicationConnector.fetchAllSubscriptions()(*))
        .willReturn(Future.successful(subscriptions))

      val result: PaginatedSubscribedApplicationResponse = await(underTest.searchApplications(Some(PRODUCTION), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.subscriptions should have size 0

      app2.subscriptions should have size 0

      app3.subscriptions should have size 0
    }

    "list all subscribed applications from sandbox when SANDBOX environment is specified" in new Setup {
      given(mockSandboxApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("sandbox-test-context", "1.0"), Seq(allSandboxApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("sandbox-unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("sandbox-super-context", "1.0"), allSandboxApplications.map(_.id.toString)))


      given(mockSandboxApplicationConnector.fetchAllSubscriptions()(*))
        .willReturn(Future.successful(subscriptions))

      val result: PaginatedSubscribedApplicationResponse = await(underTest.searchApplications(Some(SANDBOX), Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.subscriptions should have size 0

      app2.subscriptions should have size 0

      app3.subscriptions should have size 0
    }

    "list all subscribed applications from sandbox when no environment is specified" in new Setup {
      given(mockSandboxApplicationConnector.searchApplications(*)(*))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("sandbox-test-context", "1.0"), Seq(allSandboxApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("sandbox-unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("sandbox-super-context", "1.0"), allSandboxApplications.map(_.id.toString)))


      given(mockSandboxApplicationConnector.fetchAllSubscriptions()(*))
        .willReturn(Future.successful(subscriptions))

      val result: PaginatedSubscribedApplicationResponse = await(underTest.searchApplications(None, Map.empty))

      val app1 = result.applications.find(sa => sa.name == "application1").get
      val app2 = result.applications.find(sa => sa.name == "application2").get
      val app3 = result.applications.find(sa => sa.name == "application3").get

      app1.subscriptions should have size 0

      app2.subscriptions should have size 0

      app3.subscriptions should have size 0
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
      val filteredApplications = Seq(stdApp1, privilegedApp)

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
      val noSubscriptions = Seq(stdApp1, privilegedApp)
      val subscriptions = Seq(stdApp2, ropcApp)

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
      val allApps = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchAllApplications()(*)).willReturn(Future.successful(Seq.empty))

      val result = await(underTest.fetchApplications(OneOrMoreApplications, AnyEnvironment))
      result shouldBe allApps

      verify(mockProductionApplicationConnector).fetchAllApplications()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplications()(*)
    }

    "list distinct filtered applications from sandbox and production when NoSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))
      given(mockSandboxApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(*)).willReturn(Future.successful(noSubscriptions))

      val result = await(underTest.fetchApplications(NoSubscriptions, AnyEnvironment))
      result shouldBe noSubscriptions

      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(*)
    }
  }

  "fetchApplicationsByEmail" should {
    "return apps from both production and sandbox" in new Setup {
      val emailAddress = "email@example.com"
      val productionApps = Seq(stdApp1, privilegedApp)
      val sandboxApps = Seq(stdApp1.copy(deployedTo = "SANDBOX"), privilegedApp.copy(deployedTo = "SANDBOX"))

      given(mockProductionApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(productionApps))
      given(mockSandboxApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(sandboxApps))

      val result = await(underTest.fetchApplicationsByEmail(emailAddress))

      result shouldBe sandboxApps ++ productionApps
    }

    "return only distinct apps" in new Setup {
      val emailAddress = "email@example.com"
      val allApps = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchApplicationsByEmail(eqTo(emailAddress))(*)).willReturn(Future.successful(allApps))

      val result = await(underTest.fetchApplicationsByEmail(emailAddress))

      result shouldBe allApps
    }
  }

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

  "updateWhitelistedIp" should {
    "send the updated IP whitelist to the TPA connector" in new Setup {
      val existingWhitelistedIp = "192.168.1.0/24"
      val app: ApplicationResponse = stdApp1.copy(ipWhitelist = Set(existingWhitelistedIp))
      val newWhitelistedIp = "192.168.2.0/24"
      given(mockProductionApplicationConnector.manageIpWhitelist(*[ApplicationId], *)(*))
        .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))

      val result: UpdateIpWhitelistResult = await(underTest.manageWhitelistedIp(app, Set(existingWhitelistedIp, newWhitelistedIp)))

      result shouldBe UpdateIpWhitelistSuccessResult
      verify(mockProductionApplicationConnector).manageIpWhitelist(eqTo(app.id), eqTo(Set(existingWhitelistedIp, newWhitelistedIp)))(*)
    }

    "propagate connector errors" in new Setup {
      given(mockProductionApplicationConnector.manageIpWhitelist(*[ApplicationId], *)(*))
        .willReturn(Future.failed(Upstream5xxResponse("Error", 500, 500)))

      intercept[Upstream5xxResponse] {
        await(underTest.manageWhitelistedIp(stdApp1, Set("192.168.1.0/24")))
      }
    }
  }

  "subscribeToApi" should {
    val definitions = Seq(SubscriptionFieldDefinition("field1", "description", "hint", "type", "shortDescription"))

    "field definitions with empty values will persist empty values" in new Setup {
      given(mockProductionApplicationConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
          .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, ""))

      given(mockSubscriptionFieldsService.fetchFieldsValues(*, *, *)(*))
        .willReturn(Future.successful(subscriptionFieldValues))
      
      given(mockSubscriptionFieldsService.saveBlankFieldValues(*[Application], *, *, *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).subscribeToApi(eqTo(stdApp1.id), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService).fetchFieldsValues(eqTo(stdApp1), eqTo(definitions), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService).saveBlankFieldValues(eqTo(stdApp1), eqTo(context), eqTo(version), eqTo(subscriptionFieldValues))(*)
    }

    "field definitions with non-empty values will not persist anything" in new Setup {
      given(mockProductionApplicationConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
        .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, Random.nextString(length = 8)))

      given(mockSubscriptionFieldsService.fetchFieldsValues(eqTo(stdApp1), eqTo(definitions), eqTo(apiIdentifier))(*))
        .willReturn(Future.successful(subscriptionFieldValues))

      given(mockSubscriptionFieldsService.saveBlankFieldValues(*[Application], *, *, *)(*))
        .willReturn(Future.successful(SaveSubscriptionFieldsSuccessResponse))

      val fields = subscriptionFieldValues.map(v => v.definition.name -> v.value).toMap

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).subscribeToApi(eqTo(stdApp1.id), eqTo(apiIdentifier))(*)
      verify(mockSubscriptionFieldsService, never).saveFieldValues(eqTo(stdApp1), eqTo(context), eqTo(version), eqTo(fields))(*)
      verify(mockSubscriptionFieldsService).saveBlankFieldValues(eqTo(stdApp1), eqTo(context), eqTo(version), eqTo(subscriptionFieldValues))(*)
    }

    "with field definitions but fails to save subscription fields throws error" in new Setup {
      given(mockProductionApplicationConnector.subscribeToApi(*[ApplicationId], *)(*))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(*, *)(*))
          .willReturn(Future.successful(definitions))

      val subscriptionFieldValues = Seq(SubscriptionFieldValue(definitions.head, ""))

      given(mockSubscriptionFieldsService.fetchFieldsValues(*, *, *)(*))
        .willReturn(Future.successful(subscriptionFieldValues))

      val fields = subscriptionFieldValues.map(v => v.definition.name -> v.value).toMap

      val errors = Map("fieldName" -> "failure reason")

      given(mockSubscriptionFieldsService.saveBlankFieldValues(*[Application], *, *, *)(*))
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

      given(mockProductionApplicationConnector.unsubscribeFromApi(*[ApplicationId], *, *)(*))
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
    val appId = ApplicationId(UUID.randomUUID().toString())
    val clientId = "client ID"
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

  "fetchApplicationSubscriptions" should {

    "fetch subscriptions with fields" in new SubscriptionFieldsServiceSetup {
      val apiVersion = APIVersion(version, APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))
      val subscriptionFields = Seq(SubscriptionFieldValue(SubscriptionFieldDefinition("name", "description", "hint", "type", "shortDescription"), "value"))
    
      val versionsWithoutFields = Seq(VersionSubscriptionWithoutFields(apiVersion, subscribed = true))
      val subscriptionsWithoutFields = SubscriptionWithoutFields("subscription name", "service name", context, versionsWithoutFields)

      given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
      given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiIdentifier, prefetchedDefinitions))
        .willReturn(subscriptionFields)

      given(mockProductionApplicationConnector.fetchApplicationSubscriptions(stdApp1.id)).willReturn(Seq(subscriptionsWithoutFields))

      val result = await(underTest.fetchApplicationSubscriptions(stdApp1))

      val subscriptionFieldsWrapper = SubscriptionFieldsWrapper(stdApp1.id, stdApp1.clientId, context, version, subscriptionFields)
      val versions = Seq(VersionSubscription(apiVersion, subscribed = true, subscriptionFieldsWrapper))
      val subscriptions = Seq(Subscription(subscriptionsWithoutFields.name, subscriptionsWithoutFields.serviceName, context, versions))

      result shouldBe subscriptions
    }
  }

  "add teamMember" when {
    val email = "email@testuser.com"
    val teamMember = Collaborator(email, CollaboratorRole.ADMINISTRATOR)
    val adminEmail = "admin.email@example.com"
    val adminsToEmail = Set.empty[String]

    "adding to a standard app" should {
      "add an unregistered teamMember successfully in the correct environment" in new Setup {
        val application = stdApp1
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = false, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val unregisteredUser = User(email, firstName = "n/a", lastName = "n/a", verified = None)

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = stdApp1
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }
    }

    "adding to a privileged app" should {
      "add an unregistered teamMember successfully in the correct environment" in new Setup {
        val application = privilegedApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = false, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val unregisteredUser = User(email, firstName = "n/a", lastName = "n/a", verified = None)

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = privilegedApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }
    }

    "adding to an ROPC app" should {
      "add an unregistered teamMember successfully in the correct environment" in new Setup {
        val application = ropcApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = false, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val unregisteredUser = User(email, firstName = "n/a", lastName = "n/a", verified = None)

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = ropcApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }
    }

    "application connector fails" should {
      "propagate TeamMemberAlreadyExists from application connector" in new Setup {
        val existingUser = User(email, "firstName", "lastName", verified = Some(true))
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)

        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(existingUser))
        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockProductionApplicationConnector.addCollaborator(stdApp1.id, request)).willReturn(Future.failed(new TeamMemberAlreadyExists))

        intercept[TeamMemberAlreadyExists] {
          await(underTest.addTeamMember(stdApp1, teamMember, adminEmail))
        }
      }

      "propagate ApplicationNotFound from application connector" in new Setup {
        val existingUser = User(email, "firstName", "lastName", verified = Some(true))
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)

        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(existingUser))
        given(mockDeveloperConnector.fetchByEmails(*)(*)).willReturn(Future.successful(Seq.empty))
        given(mockProductionApplicationConnector.addCollaborator(stdApp1.id, request)).willReturn(Future.failed(new ApplicationNotFound))

        intercept[ApplicationNotFound] {
          await(underTest.addTeamMember(stdApp1, teamMember, adminEmail))
        }
      }
    }

    "building parameters" should {
      "include correct set of admins to email" in new Setup {
        val verifiedAdmin = Collaborator("verified@example.com", CollaboratorRole.ADMINISTRATOR)
        val unverifiedAdmin = Collaborator("unverified@example.com", CollaboratorRole.ADMINISTRATOR)
        val adderAdmin = Collaborator(adminEmail, CollaboratorRole.ADMINISTRATOR)
        val verifiedDeveloper = Collaborator("developer@example.com", CollaboratorRole.DEVELOPER)
        val application = stdApp1.copy(collaborators = Set(verifiedAdmin, unverifiedAdmin, adderAdmin, verifiedDeveloper))
        val nonAdderAdmins = Seq(
          User(verifiedAdmin.emailAddress, "verified", "user", Some(true)),
          User(unverifiedAdmin.emailAddress, "unverified", "user", Some(false)))
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = false, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val newUser = User(email, "n/a", "n/a", verified = None)

        given(mockDeveloperConnector.fetchByEmail(email))
          .willReturn(Future.successful(newUser))
        given(mockDeveloperConnector.fetchByEmails(eqTo(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(*))
          .willReturn(Future.successful(nonAdderAdmins))
        given(mockProductionApplicationConnector.addCollaborator(*[ApplicationId], *)(*))
          .willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adderAdmin.emailAddress)) shouldBe response

        verify(mockProductionApplicationConnector)
          .addCollaborator(eqTo(application.id), eqTo(request.copy(adminsToEmail = Set(verifiedAdmin.emailAddress))))(*)
      }
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
