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
import model._
import model.SubscriptionFields._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => mEq, _}
import org.mockito.Mockito.{never, spy, verify}
import org.scalatest.mockito.MockitoSugar
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream5xxResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

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
      UUID.randomUUID(), "clientid1", "gatewayId1", "application1", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val stdApp2 = ApplicationResponse(
      UUID.randomUUID(), "clientid2", "gatewayId2", "application2", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    val privilegedApp = ApplicationResponse(
      UUID.randomUUID(), "clientid3", "gatewayId3", "application3", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Privileged(), ApplicationState())
    val ropcApp = ApplicationResponse(
      UUID.randomUUID(), "clientid4", "gatewayId4", "application4", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Ropc(), ApplicationState())
    val applicationWithHistory = ApplicationWithHistory(stdApp1, Seq.empty)
    val gatekeeperUserId = "loggedin.gatekeeper"

    val apiContextVersion = ApiContextVersion("a-context","1.0")
    val context = apiContextVersion.context
    val version = apiContextVersion.version

    val allProductionApplications = Seq(stdApp1, stdApp2, privilegedApp)
    val allSandboxApplications = allProductionApplications.map(_.copy(id = UUID.randomUUID, deployedTo = "SANDBOX"))
  }

  trait SubscriptionFieldsServiceSetup  extends Setup {
    val prefetchedDefinitions : DefinitionsByApiVersion = Map(apiContextVersion -> Seq(SubscriptionFieldDefinition("name", "description", "hint", "String")))

    def subscriptionFields : Seq[SubscriptionFieldValue]

    given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
    given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiContextVersion, prefetchedDefinitions))
      .willReturn(subscriptionFields)
  }


  "searchApplications" should {

    "list all subscribed applications from production when PRODUCTION environment is specified" in new Setup {
      given(mockProductionApplicationConnector.searchApplications(any())(any[HeaderCarrier]))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allProductionApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("test-context", "1.0"), Seq(allProductionApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("super-context", "1.0"), allProductionApplications.map(_.id.toString)))

      given(mockProductionApplicationConnector.fetchAllSubscriptions()(any[HeaderCarrier]))
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
      given(mockSandboxApplicationConnector.searchApplications(any())(any[HeaderCarrier]))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("sandbox-test-context", "1.0"), Seq(allSandboxApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("sandbox-unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("sandbox-super-context", "1.0"), allSandboxApplications.map(_.id.toString)))


      given(mockSandboxApplicationConnector.fetchAllSubscriptions()(any[HeaderCarrier]))
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
      given(mockSandboxApplicationConnector.searchApplications(any())(any[HeaderCarrier]))
        .willReturn(Future.successful(aPaginatedApplicationResponse(allSandboxApplications)))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("sandbox-test-context", "1.0"), Seq(allSandboxApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("sandbox-unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("sandbox-super-context", "1.0"), allSandboxApplications.map(_.id.toString)))


      given(mockSandboxApplicationConnector.fetchAllSubscriptions()(any[HeaderCarrier]))
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
      val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
      val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

      given(mockProductionApplicationConnector.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
        .willReturn(Future.successful(ResendVerificationSuccessful))

      val result = await(underTest.resendVerification(stdApp1, userName))

      appIdCaptor.getValue shouldBe stdApp1.id.toString
      gatekeeperIdCaptor.getValue shouldBe userName
    }
  }

  "fetchApplications" should {

    "list all applications from sandbox and production when filtering not provided" in new Setup {
      given(mockProductionApplicationConnector.fetchAllApplications()(any()))
        .willReturn(Future.successful(allProductionApplications))
      given(mockSandboxApplicationConnector.fetchAllApplications()(any()))
        .willReturn(Future.successful(allProductionApplications))

      val result: Seq[ApplicationResponse] = await(underTest.fetchApplications)
      result shouldEqual allProductionApplications

      verify(mockProductionApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
    }

    "list filtered applications from sandbox and production when specific subscription filtering is provided" in new Setup {
      val filteredApplications = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplicationsBySubscription(any(), any())(any()))
        .willReturn(Future.successful(filteredApplications))
      given(mockSandboxApplicationConnector.fetchAllApplicationsBySubscription(any(), any())(any()))
        .willReturn(Future.successful(filteredApplications))

      val result = await(underTest.fetchApplications(Value("subscription", "version"), AnyEnvironment))
      result shouldBe filteredApplications

      verify(mockProductionApplicationConnector).fetchAllApplicationsBySubscription(mEq("subscription"), mEq("version"))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplicationsBySubscription(mEq("subscription"), mEq("version"))(any[HeaderCarrier])
    }

    "list filtered applications from sandbox and production when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(stdApp1, privilegedApp)
      val subscriptions = Seq(stdApp2, ropcApp)

      val allApps = noSubscriptions ++ subscriptions
      given(mockProductionApplicationConnector.fetchAllApplications()(any())).willReturn(Future.successful(allApps))
      given(mockProductionApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(any())).willReturn(Future.successful(noSubscriptions))
      given(mockSandboxApplicationConnector.fetchAllApplications()(any())).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(any())).willReturn(Future.successful(noSubscriptions))

      val result = await(underTest.fetchApplications(OneOrMoreSubscriptions, AnyEnvironment))
      result shouldBe subscriptions

      verify(mockProductionApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier])
    }

    "list filtered applications from sandbox and production when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplications()(any())).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchAllApplications()(any())).willReturn(Future.successful(Seq.empty))

      val result = await(underTest.fetchApplications(OneOrMoreApplications, AnyEnvironment))
      result shouldBe allApps

      verify(mockProductionApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplications()(any[HeaderCarrier])
    }

    "list distinct filtered applications from sandbox and production when NoSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(any())).willReturn(Future.successful(noSubscriptions))
      given(mockSandboxApplicationConnector.fetchAllApplicationsWithNoSubscriptions()(any())).willReturn(Future.successful(noSubscriptions))

      val result = await(underTest.fetchApplications(NoSubscriptions, AnyEnvironment))
      result shouldBe noSubscriptions

      verify(mockProductionApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier])
    }
  }

  "fetchApplicationsByEmail" should {
    "return apps from both production and sandbox" in new Setup {
      val emailAddress = "email@example.com"
      val productionApps = Seq(stdApp1, privilegedApp)
      val sandboxApps = Seq(stdApp1.copy(deployedTo = "SANDBOX"), privilegedApp.copy(deployedTo = "SANDBOX"))

      given(mockProductionApplicationConnector.fetchApplicationsByEmail(mEq(emailAddress))(any[HeaderCarrier])).willReturn(Future.successful(productionApps))
      given(mockSandboxApplicationConnector.fetchApplicationsByEmail(mEq(emailAddress))(any[HeaderCarrier])).willReturn(Future.successful(sandboxApps))

      val result = await(underTest.fetchApplicationsByEmail(emailAddress))

      result shouldBe sandboxApps ++ productionApps
    }

    "return only distinct apps" in new Setup {
      val emailAddress = "email@example.com"
      val allApps = Seq(stdApp1, privilegedApp)

      given(mockProductionApplicationConnector.fetchApplicationsByEmail(mEq(emailAddress))(any[HeaderCarrier])).willReturn(Future.successful(allApps))
      given(mockSandboxApplicationConnector.fetchApplicationsByEmail(mEq(emailAddress))(any[HeaderCarrier])).willReturn(Future.successful(allApps))

      val result = await(underTest.fetchApplicationsByEmail(emailAddress))

      result shouldBe allApps
    }
  }

  "fetchApplication" should {
    "return the app when found in production" in new Setup {
      given(mockProductionApplicationConnector.fetchApplication(anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(applicationWithHistory))
      given(mockSandboxApplicationConnector.fetchApplication(anyString)(any[HeaderCarrier]))
        .willReturn(Future.failed(new NotFoundException("Not Found")))

      val result = await(underTest.fetchApplication(stdApp1.id.toString))

      result shouldBe applicationWithHistory

      verify(mockProductionApplicationConnector).fetchApplication(mEq(stdApp1.id.toString))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector, never).fetchApplication(anyString)(any[HeaderCarrier])
    }

    "return the the app in sandbox when not found in production" in new Setup {
      given(mockProductionApplicationConnector.fetchApplication(anyString)(any[HeaderCarrier]))
        .willReturn(Future.failed(new NotFoundException("Not Found")))
      given(mockSandboxApplicationConnector.fetchApplication(anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(applicationWithHistory))

      val result = await(underTest.fetchApplication(stdApp1.id.toString))

      verify(mockProductionApplicationConnector).fetchApplication(mEq(stdApp1.id.toString))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector).fetchApplication(mEq(stdApp1.id.toString))(any[HeaderCarrier])
    }
  }

  "updateOverrides" should {
    "call the service to update the overrides for an app with Standard access" in new Setup {
      given(mockProductionApplicationConnector.updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateOverridesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(ApiScope("test.key", "test name", "test description"))))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin(), SuppressIvForAgents(Set("test.key")))))

      result shouldBe UpdateOverridesSuccessResult

      verify(mockProductionApplicationConnector).updateOverrides(mEq(stdApp1.id.toString),
        mEq(UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("test.key"))))))(any[HeaderCarrier])
    }

    "fail when called with invalid scopes" in new Setup {
      given(mockProductionApiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(ApiScope("test.key", "test name", "test description"))))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin(), SuppressIvForAgents(Set("test.key", "invalid.key")))))

      result shouldBe UpdateOverridesFailureResult(Set(SuppressIvForAgents(Set("test.key", "invalid.key"))))

      verify(mockProductionApplicationConnector, never).updateOverrides(any(), any())(any())
    }

    "fail when called for an app with Privileged access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(privilegedApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier])
    }

    "fail when called for an app with ROPC access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(ropcApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(mockProductionApplicationConnector, never).updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier])
    }
  }

  "updateScopes" should {
    "call the service to update the scopes for an app with Privileged access" in new Setup {
      given(mockProductionApplicationConnector.updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateScopesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(
          ApiScope("hello", "test name", "test description"),
          ApiScope("individual-benefits", "test name", "test description"))))

      val result = await(underTest.updateScopes(privilegedApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(mEq(privilegedApp.id.toString),
        mEq(UpdateScopesRequest(Set("hello", "individual-benefits"))))(any[HeaderCarrier])
    }

    "call the service to update the scopes for an app with ROPC access" in new Setup {
      given(mockProductionApplicationConnector.updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateScopesSuccessResult))
      given(mockProductionApiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(
          ApiScope("hello", "test name", "test description"),
          ApiScope("individual-benefits", "test name", "test description"))))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(mockProductionApplicationConnector).updateScopes(mEq(ropcApp.id.toString),
        mEq(UpdateScopesRequest(Set("hello", "individual-benefits"))))(any[HeaderCarrier])
    }

    "fail when called with invalid scopes" in new Setup {
      given(mockProductionApiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(ApiScope("hello", "test name", "test description"))))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesInvalidScopesResult

      verify(mockProductionApplicationConnector, never).updateScopes(any(), any())(any())
    }

    "fail when called for an app with Standard access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateScopes(stdApp1, Set("hello", "individual-benefits")))
      }

      verify(mockProductionApplicationConnector, never).updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier])
    }
  }

  "updateWhitelistedIp" should {
    "send the updated IP whitelist to the TPA connector" in new Setup {
      val existingWhitelistedIp = "192.168.1.0/24"
      val app: ApplicationResponse = stdApp1.copy(ipWhitelist = Set(existingWhitelistedIp))
      val newWhitelistedIp = "192.168.2.0/24"
      given(mockProductionApplicationConnector.manageIpWhitelist(anyString, any[Set[String]])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))

      val result: UpdateIpWhitelistResult = await(underTest.manageWhitelistedIp(app, Set(existingWhitelistedIp, newWhitelistedIp)))

      result shouldBe UpdateIpWhitelistSuccessResult
      verify(mockProductionApplicationConnector).manageIpWhitelist(mEq(app.id.toString), mEq(Set(existingWhitelistedIp, newWhitelistedIp)))(any[HeaderCarrier])
    }

    "propagate connector errors" in new Setup {
      given(mockProductionApplicationConnector.manageIpWhitelist(anyString, any[Set[String]])(any[HeaderCarrier]))
        .willReturn(Future.failed(Upstream5xxResponse("Error", 500, 500)))

      intercept[Upstream5xxResponse] {
        await(underTest.manageWhitelistedIp(stdApp1, Set("192.168.1.0/24")))
      }
    }
  }

  "subscribeToApi" should {
    "field definitions with empty values will persist empty values" in new Setup {
      private val apiIdentifier: APIIdentifier = APIIdentifier(context, version)

      given(mockProductionApplicationConnector.subscribeToApi(anyString, any[APIIdentifier])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val definitions = Seq(SubscriptionFieldDefinition("field1", "description", "hint", "type"))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(any(), any())(any[HeaderCarrier]))
          .willReturn(Future.successful(definitions))

      val subscriptionFieldValues: Seq[SubscriptionFieldValue] = Seq(SubscriptionFieldValue(definitions.head, ""))

      given(mockSubscriptionFieldsService.fetchFieldsValues(mEq(stdApp1), mEq(definitions), mEq(apiIdentifier))(any[ExecutionContext], any[HeaderCarrier]))
        .willReturn(Future.successful(subscriptionFieldValues))

      val fields = subscriptionFieldValues.map(v => v.definition.name -> v.value).toMap

      given(mockSubscriptionFieldsService.saveFieldValues(mEq(stdApp1), mEq(context), mEq(version), mEq(fields))(any[HeaderCarrier]))
          .willReturn(Future.successful(HttpResponse(200)))

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).subscribeToApi(mEq(stdApp1.id.toString), mEq(apiIdentifier))(any[HeaderCarrier])
      verify(mockSubscriptionFieldsService).saveFieldValues(mEq(stdApp1), mEq(context), mEq(version), mEq(fields))(any[HeaderCarrier])
    }

    "field definitions with non-empty values will not persist anything" in new Setup {
      private val apiIdentifier: APIIdentifier = APIIdentifier(context, version)

      given(mockProductionApplicationConnector.subscribeToApi(anyString, any[APIIdentifier])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val definitions = Seq(SubscriptionFieldDefinition("field1", "description", "hint", "type"))

      given(mockSubscriptionFieldsService.fetchFieldDefinitions(any(), any())(any[HeaderCarrier]))
        .willReturn(Future.successful(definitions))

      val subscriptionFieldValues: Seq[SubscriptionFieldValue] = Seq(SubscriptionFieldValue(definitions.head, Random.nextString(8)))

      given(mockSubscriptionFieldsService.fetchFieldsValues(mEq(stdApp1), mEq(definitions), mEq(apiIdentifier))(any[ExecutionContext], any[HeaderCarrier]))
        .willReturn(Future.successful(subscriptionFieldValues))

      val fields = subscriptionFieldValues.map(v => v.definition.name -> v.value).toMap

      val result = await(underTest.subscribeToApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).subscribeToApi(mEq(stdApp1.id.toString), mEq(apiIdentifier))(any[HeaderCarrier])
      verify(mockSubscriptionFieldsService, never).saveFieldValues(mEq(stdApp1), mEq(context), mEq(version), mEq(fields))(any[HeaderCarrier])
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API and delete the field values" in new Setup {

      given(mockProductionApplicationConnector.unsubscribeFromApi(anyString, anyString, anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))
      given(mockSubscriptionFieldsService.deleteFieldValues(any[Application], anyString, anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(FieldsDeleteSuccessResult))

      val result = await(underTest.unsubscribeFromApi(stdApp1, context, version))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).unsubscribeFromApi(mEq(stdApp1.id.toString), mEq(context), mEq(version))(any[HeaderCarrier])
      verify(mockSubscriptionFieldsService).deleteFieldValues(mEq(stdApp1), mEq(context), mEq(version))(any[HeaderCarrier])
    }
  }

  "updateRateLimitTier" should {
    "call the service to update the rate limit tier" in new Setup {
      given(mockProductionApplicationConnector.updateRateLimitTier(any[String], any[RateLimitTier])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))


      val result = await(underTest.updateRateLimitTier(stdApp1, RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult

      verify(mockProductionApplicationConnector).updateRateLimitTier(mEq(stdApp1.id.toString), mEq(RateLimitTier.GOLD))(any[HeaderCarrier])
    }
  }

  "createPrivOrROPCApp" should {
    val admin = Seq(Collaborator("admin@example.com", CollaboratorRole.ADMINISTRATOR))
    val totpSecrets = Some(TotpSecrets("secret"))
    val appAccess = AppAccess(AccessType.PRIVILEGED, Seq())

    val name = "New app"
    val appId = "app ID"
    val clientId = "client ID"
    val description = "App description"

    "call the production connector to create a new app in production" in new Setup {
      val environment = Environment.PRODUCTION

      given(mockProductionApplicationConnector.createPrivOrROPCApp(any[CreatePrivOrROPCAppRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)))


      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockProductionApplicationConnector)
        .createPrivOrROPCApp(mEq(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector, never).createPrivOrROPCApp(any())(any())
    }

    "call the sandbox connector to create a new app in sandbox" in new Setup {
      val environment = Environment.SANDBOX

      given(mockSandboxApplicationConnector.createPrivOrROPCApp(any[CreatePrivOrROPCAppRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)))


      val result = await(underTest.createPrivOrROPCApp(environment, name, description, admin, appAccess))

      result shouldBe CreatePrivOrROPCAppSuccessResult(appId, name, environment.toString, clientId, totpSecrets, appAccess)

      verify(mockSandboxApplicationConnector)
        .createPrivOrROPCApp(mEq(CreatePrivOrROPCAppRequest(environment.toString, name, description, admin, appAccess)))(any[HeaderCarrier])
      verify(mockProductionApplicationConnector, never).createPrivOrROPCApp(any())(any())
    }
  }

  "fetchApplicationSubscriptions" should {

    "fetch subscriptions without fields" in new SubscriptionFieldsServiceSetup {
      val versionFields = None
      val subscriptionFields = Seq()
      val apiVersion = APIVersion(version, APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))
      val versions = Seq(VersionSubscription(apiVersion, subscribed = true, versionFields))
      val subscriptions = Seq(Subscription("subscription name", "service name", context, versions))

      given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
      given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiContextVersion, prefetchedDefinitions))
        .willReturn(subscriptionFields)

      given(mockProductionApplicationConnector.fetchApplicationSubscriptions(stdApp1.id.toString)).willReturn(subscriptions)

      val result = await(underTest.fetchApplicationSubscriptions(stdApp1))

      result shouldBe subscriptions

      verify(mockSubscriptionFieldsService,never()).fetchAllFieldDefinitions(any())(any[HeaderCarrier])
    }

    "fetch subscriptions with fields" in new SubscriptionFieldsServiceSetup {
      val apiVersion = APIVersion(version, APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))
      val subscriptionFields = Seq(SubscriptionFieldValue(SubscriptionFieldDefinition("name", "description", "hint", "type"), "value"))
      val subscriptionFieldsWrapper = SubscriptionFieldsWrapper(stdApp1.id.toString, stdApp1.clientId, context, version, subscriptionFields)
      val versions = Seq(VersionSubscription(apiVersion, subscribed = true, Some(subscriptionFieldsWrapper)))
      val subscriptions = Seq(Subscription("subscription name", "service name", context, versions))

      given(mockSubscriptionFieldsService.fetchAllFieldDefinitions(stdApp1.deployedTo)).willReturn(prefetchedDefinitions)
      given(mockSubscriptionFieldsService.fetchFieldsWithPrefetchedDefinitions(stdApp1, apiContextVersion, prefetchedDefinitions))
        .willReturn(subscriptionFields)

      given(mockProductionApplicationConnector.fetchApplicationSubscriptions(stdApp1.id.toString)).willReturn(subscriptions)

      val result = await(underTest.fetchApplicationSubscriptions(stdApp1, withFields = true))

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

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = stdApp1
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

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

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = privilegedApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

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

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(unregisteredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }

      "add a registered teamMember successfully in the correct environment" in new Setup {
        val application = ropcApp
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)
        val response = ApplicationUpdateSuccessResult
        val registeredUser = User(email, "firstName", "lastName", verified = Some(true))

        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(registeredUser))
        given(mockProductionApplicationConnector.addCollaborator(application.id.toString, request)).willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adminEmail)) shouldBe response
        verify(underTest).applicationConnectorFor(application)
      }
    }

    "application connector fails" should {
      "propagate TeamMemberAlreadyExists from application connector" in new Setup {
        val existingUser = User(email, "firstName", "lastName", verified = Some(true))
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)

        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(existingUser))
        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockProductionApplicationConnector.addCollaborator(stdApp1.id.toString, request)).willReturn(Future.failed(new TeamMemberAlreadyExists))

        intercept[TeamMemberAlreadyExists] {
          await(underTest.addTeamMember(stdApp1, teamMember, adminEmail))
        }
      }

      "propagate ApplicationNotFound from application connector" in new Setup {
        val existingUser = User(email, "firstName", "lastName", verified = Some(true))
        val request = AddTeamMemberRequest(adminEmail, teamMember, isRegistered = true, adminsToEmail)

        given(mockDeveloperConnector.fetchByEmail(email)).willReturn(Future.successful(existingUser))
        given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
        given(mockProductionApplicationConnector.addCollaborator(stdApp1.id.toString, request)).willReturn(Future.failed(new ApplicationNotFound))

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
        given(mockDeveloperConnector.fetchByEmails(mEq(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(any()))
          .willReturn(Future.successful(nonAdderAdmins))
        given(mockProductionApplicationConnector.addCollaborator(any(), any())(any()))
          .willReturn(response)

        await(underTest.addTeamMember(application, teamMember, adderAdmin.emailAddress)) shouldBe response

        verify(mockProductionApplicationConnector)
          .addCollaborator(mEq(application.id.toString), mEq(request.copy(adminsToEmail = Set(verifiedAdmin.emailAddress))))(any())
      }
    }
  }

  "removeTeamMember" should {
    val requestingUser = "admin.email@example.com"
    val memberToRemove = "email@testuser.com"

    "remove a member from a standard app successfully in the correct environment" in new Setup {
      val application = stdApp1
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any()))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any())
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from a privileged app in the correct environment" in new Setup {
      val application = privilegedApp
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any()))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any())
      verify(underTest).applicationConnectorFor(application)
    }

    "remove a member from an ROPC app in the correct environment" in new Setup {
      val application = ropcApp
      val response = ApplicationUpdateSuccessResult

      given(mockDeveloperConnector.fetchByEmails(any())(any())).willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any()))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, requestingUser)) shouldBe response

      verify(mockProductionApplicationConnector).removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), any())(any())
      verify(underTest).applicationConnectorFor(application)
    }

    "propagate TeamMemberLastAdmin error from application connector" in new Setup {
      val lastAdmin = User(memberToRemove, "firstName", "lastName", verified = Some(true))

      given(mockDeveloperConnector.fetchByEmails(any())(any()))
        .willReturn(Future.successful(Seq.empty))
      given(mockProductionApplicationConnector.removeCollaborator(mEq(stdApp1.id.toString), mEq(memberToRemove), mEq(requestingUser), mEq(Seq.empty))(any()))
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

      given(mockDeveloperConnector.fetchByEmails(mEq(Set(verifiedAdmin.emailAddress, unverifiedAdmin.emailAddress)))(any()))
        .willReturn(Future.successful(nonAdderAdmins))
      given(mockProductionApplicationConnector.removeCollaborator(any(), any(), any(), any())(any()))
        .willReturn(response)

      await(underTest.removeTeamMember(application, memberToRemove, adderAdmin.emailAddress)) shouldBe response

      verify(mockProductionApplicationConnector)
        .removeCollaborator(mEq(application.id.toString), mEq(memberToRemove), mEq(requestingUser), mEq(expectedAdminsToEmail))(any())
    }
  }

  "approveUplift" should {
    "approve the uplift in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")

      given(mockProductionApplicationConnector.approveUplift(anyString, anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(ApproveUpliftSuccessful))

      val result = await(underTest.approveUplift(application, gatekeeperUserId))

      result shouldBe ApproveUpliftSuccessful

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).approveUplift(mEq(application.id.toString), mEq(gatekeeperUserId))(any[HeaderCarrier])
    }
  }

  "rejectUplift" should {
    "reject the uplift in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val rejectionReason = "Rejected"

      given(mockSandboxApplicationConnector.rejectUplift(anyString, anyString, anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(RejectUpliftSuccessful))

      val result = await(underTest.rejectUplift(application, gatekeeperUserId, rejectionReason))

      result shouldBe RejectUpliftSuccessful

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).rejectUplift(mEq(application.id.toString), mEq(gatekeeperUserId), mEq(rejectionReason))(any[HeaderCarrier])
    }
  }

  "deleteApplication" should {
    "delete the application in the correct environment" in new Setup {
      val emailAddress = "email@example.com"
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val deleteApplicationRequest = DeleteApplicationRequest(gatekeeperUserId, emailAddress)

      given(mockProductionApplicationConnector.deleteApplication(anyString, any[DeleteApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationDeleteSuccessResult))

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationDeleteSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).deleteApplication(mEq(application.id.toString), mEq(deleteApplicationRequest))(any[HeaderCarrier])
    }

    "propagate ApplicationDeleteFailureResult from connector" in new Setup {
      val emailAddress = "email@example.com"
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val deleteApplicationRequest = DeleteApplicationRequest(gatekeeperUserId, emailAddress)

      given(mockSandboxApplicationConnector.deleteApplication(anyString, any[DeleteApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationDeleteFailureResult))

      val result = await(underTest.deleteApplication(application, gatekeeperUserId, emailAddress))

      result shouldBe ApplicationDeleteFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).deleteApplication(mEq(application.id.toString), mEq(deleteApplicationRequest))(any[HeaderCarrier])
    }
  }

  "blockApplication" should {
    "block the application in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val blockApplicationRequest = BlockApplicationRequest(gatekeeperUserId)

      given(mockProductionApplicationConnector.blockApplication(anyString, any[BlockApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationBlockSuccessResult))

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).blockApplication(mEq(application.id.toString), mEq(blockApplicationRequest))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector, never).blockApplication(any(), any())(any())
    }

    "propagate ApplicationBlockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val blockApplicationRequest = BlockApplicationRequest(gatekeeperUserId)

      given(mockSandboxApplicationConnector.blockApplication(anyString, any[BlockApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationBlockFailureResult))

      val result = await(underTest.blockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationBlockFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).blockApplication(mEq(application.id.toString), mEq(blockApplicationRequest))(any[HeaderCarrier])
      verify(mockProductionApplicationConnector, never).blockApplication(any(), any())(any())
    }
  }

  "unblockApplication" should {
    "unblock the application in the correct environment" in new Setup {
      val application = stdApp1.copy(deployedTo = "PRODUCTION")
      val unblockApplicationRequest = UnblockApplicationRequest(gatekeeperUserId)

      given(mockProductionApplicationConnector.unblockApplication(anyString, any[UnblockApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUnblockSuccessResult))

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockSuccessResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockProductionApplicationConnector).unblockApplication(mEq(application.id.toString), mEq(unblockApplicationRequest))(any[HeaderCarrier])
      verify(mockSandboxApplicationConnector, never).unblockApplication(any(), any())(any())
    }

    "propagate ApplicationUnblockFailureResult from connector" in new Setup {
      val application = stdApp1.copy(deployedTo = "SANDBOX")
      val unblockApplicationRequest = UnblockApplicationRequest(gatekeeperUserId)

      given(mockSandboxApplicationConnector.unblockApplication(anyString, any[UnblockApplicationRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUnblockFailureResult))

      val result = await(underTest.unblockApplication(application, gatekeeperUserId))

      result shouldBe ApplicationUnblockFailureResult

      verify(underTest).applicationConnectorFor(application)
      verify(mockSandboxApplicationConnector).unblockApplication(mEq(application.id.toString), mEq(unblockApplicationRequest))(any[HeaderCarrier])
      verify(mockProductionApplicationConnector, never).unblockApplication(any(), any())(any())
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
