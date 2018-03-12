/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.services

import java.util.UUID

import connectors.{ApiScopeConnector, ApplicationConnector}
import model.RateLimitTier.RateLimitTier
import model._
import org.joda.time.DateTime
import org.mockito.Mockito.{never, verify}
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => mEq, _}
import org.scalatest.mockito.MockitoSugar
import services.ApplicationService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val underTest = new ApplicationService {
      val applicationConnector = mock[ApplicationConnector]
      val apiScopeConnector = mock[ApiScopeConnector]
    }

    implicit val hc = HeaderCarrier()

    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    val stdApp1 = ApplicationResponse(UUID.randomUUID(), "application1", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())
    val stdApp2 = ApplicationResponse(UUID.randomUUID(), "application2", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())
    val privilegedApp = ApplicationResponse(UUID.randomUUID(), "application3", "PRODUCTION", None, collaborators, DateTime.now(), Privileged(), ApplicationState())
    val ropcApp = ApplicationResponse(UUID.randomUUID(), "application4", "PRODUCTION", None, collaborators, DateTime.now(), Ropc(), ApplicationState())

    val allApplications = Seq(stdApp1, stdApp2, privilegedApp)
  }

  "fetchAllSubscribedApplications" should {

    "list all subscribed applications" in new Setup {
      given(underTest.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApplications))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("test-context", "1.0"), Seq(allApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("super-context", "1.0"), allApplications.map(_.id.toString)))


      given(underTest.applicationConnector.fetchAllSubscriptions()(any[HeaderCarrier]))
        .willReturn(Future.successful(subscriptions))


      val result: Seq[SubscribedApplicationResponse] = await(underTest.fetchAllSubscribedApplications)

      val app1 = result.find(sa => sa.name == "application1").get
      val app2 = result.find(sa => sa.name == "application2").get
      val app3 = result.find(sa => sa.name == "application3").get

      app1.subscriptions should have size 1
      app1.subscriptions shouldBe Seq(SubscriptionNameAndVersion("super-context","1.0"))

      app2.subscriptions should have size 2
      app2.subscriptions shouldBe Seq(
        SubscriptionNameAndVersion("super-context", "1.0"),
        SubscriptionNameAndVersion("test-context", "1.0")
      )

      app3.subscriptions should have size 1
      app3.subscriptions shouldBe Seq(SubscriptionNameAndVersion("super-context", "1.0"))
    }
  }

  "resendVerification" should {

    "call applicationConnector with appropriate parameters" in new Setup {
      val applicationId = "applicationId"
      val userName = "userName"
      val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
      val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

      given(underTest.applicationConnector.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
        .willReturn(Future.successful(ResendVerificationSuccessful))

      val result = await(underTest.resendVerification(applicationId, userName))

      appIdCaptor.getValue shouldBe applicationId
      gatekeeperIdCaptor.getValue shouldBe userName
    }
  }

  "fetchApplications" should {

    "list all applications when filtering not provided" in new Setup {
      given(underTest.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApplications))

      val result: Seq[ApplicationResponse] = await(underTest.fetchApplications)
      result shouldEqual allApplications
    }

    "list filtered applications when specific subscription filtering is provided" in new Setup {
      val filteredApplications = Seq(stdApp1, privilegedApp)

      given(underTest.applicationConnector.fetchAllApplicationsBySubscription(mEq("subscription"))(any[HeaderCarrier]))
        .willReturn(Future.successful(filteredApplications))

      val result = await(underTest.fetchApplications(Value("subscription")))
      result shouldBe filteredApplications
    }

    "list filtered applications when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(stdApp1, privilegedApp)

      val subscriptions = Seq(stdApp2, ropcApp)

      val allApps = noSubscriptions ++ subscriptions
      given(underTest.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApps))
      given(underTest.applicationConnector.fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier]))
        .willReturn(Future.successful(noSubscriptions))
      val result = await(underTest.fetchApplications(OneOrMoreSubscriptions))
      result shouldBe subscriptions
    }

    "list filtered applications when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = Seq(stdApp1, privilegedApp)

      given(underTest.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApps))
      val result = await(underTest.fetchApplications(OneOrMoreApplications))
      result shouldBe allApps
    }
  }

  "updateOverrides" should {
    "call the service to update the overrides for an app with Standard access" in new Setup {
      given(underTest.applicationConnector.updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateOverridesSuccessResult))
      given(underTest.apiScopeConnector.fetchAll()(any[HeaderCarrier]))
        .willReturn(Future.successful(Seq(ApiScope("test.key", "test name", "test description"))))

      val result = await(underTest.updateOverrides(stdApp1, Set(PersistLogin(), SuppressIvForAgents(Set("test.key")))))

      result shouldBe UpdateOverridesSuccessResult

      verify(underTest.applicationConnector).updateOverrides(mEq(stdApp1.id.toString),
        mEq(UpdateOverridesRequest(Set(PersistLogin(), SuppressIvForAgents(Set("test.key"))))))(any[HeaderCarrier])
    }

    "fail when called for an app with Privileged access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(privilegedApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(underTest.applicationConnector, never).updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier])
    }

    "fail when called for an app with ROPC access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateOverrides(ropcApp, Set(PersistLogin(), SuppressIvForAgents(Set("hello")))))
      }

      verify(underTest.applicationConnector, never).updateOverrides(anyString, any[UpdateOverridesRequest])(any[HeaderCarrier])
    }
  }

  "updateScopes" should {
    "call the service to update the scopes for an app with Privileged access" in new Setup {
      given(underTest.applicationConnector.updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateScopesSuccessResult))

      val result = await(underTest.updateScopes(privilegedApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(underTest.applicationConnector).updateScopes(mEq(privilegedApp.id.toString),
        mEq(UpdateScopesRequest(Set("hello", "individual-benefits"))))(any[HeaderCarrier])
    }

    "call the service to update the scopes for an app with ROPC access" in new Setup {
      given(underTest.applicationConnector.updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier]))
        .willReturn(Future.successful(UpdateScopesSuccessResult))

      val result = await(underTest.updateScopes(ropcApp, Set("hello", "individual-benefits")))

      result shouldBe UpdateScopesSuccessResult

      verify(underTest.applicationConnector).updateScopes(mEq(ropcApp.id.toString),
        mEq(UpdateScopesRequest(Set("hello", "individual-benefits"))))(any[HeaderCarrier])
    }

    "fail when called for an app with Standard access" in new Setup {
      intercept[RuntimeException] {
        await(underTest.updateScopes(stdApp1, Set("hello", "individual-benefits")))
      }

      verify(underTest.applicationConnector, never).updateScopes(anyString, any[UpdateScopesRequest])(any[HeaderCarrier])
    }
  }

  "subscribeToApi" should {
    "call the service to subscribe to the API" in new Setup {
      given(underTest.applicationConnector.subscribeToApi(anyString, any[APIIdentifier])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val result = await(underTest.subscribeToApi("applicationId", "hello", "1.0"))

      result shouldBe ApplicationUpdateSuccessResult

      verify(underTest.applicationConnector).subscribeToApi(mEq("applicationId"), mEq(APIIdentifier("hello", "1.0")))(any[HeaderCarrier])
    }
  }

  "unsubscribeFromApi" should {
    "call the service to unsubscribe from the API" in new Setup {
      given(underTest.applicationConnector.unsubscribeFromApi(anyString, anyString, anyString)(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val result = await(underTest.unsubscribeFromApi("applicationId", "hello", "1.0"))

      result shouldBe ApplicationUpdateSuccessResult

      verify(underTest.applicationConnector).unsubscribeFromApi(mEq("applicationId"), mEq("hello"), mEq("1.0"))(any[HeaderCarrier])
    }
  }

  "updateRateLimitTier" should {
    "call the service to update the rate limit tier" in new Setup {
      given(underTest.applicationConnector.updateRateLimitTier(anyString, any[RateLimitTier])(any[HeaderCarrier]))
        .willReturn(Future.successful(ApplicationUpdateSuccessResult))

      val result = await(underTest.updateRateLimitTier("applicationId", RateLimitTier.GOLD))

      result shouldBe ApplicationUpdateSuccessResult

      verify(underTest.applicationConnector).updateRateLimitTier(mEq("applicationId"), mEq(RateLimitTier.GOLD))(any[HeaderCarrier])
    }
  }
}
