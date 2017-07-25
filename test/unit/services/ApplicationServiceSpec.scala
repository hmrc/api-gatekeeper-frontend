/*
 * Copyright 2017 HM Revenue & Customs
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

import connectors.ApplicationConnector
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => mEq, _}
import org.scalatest.mock.MockitoSugar
import services.ApplicationService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val testApplicationService = new ApplicationService {
      val applicationConnector = mock[ApplicationConnector]
    }
    implicit val hc = HeaderCarrier()

    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    val allApplications = Seq(
      ApplicationResponse(UUID.randomUUID(),
        "application1", None, collaborators, DateTime.now(), ApplicationState()),
      ApplicationResponse(UUID.randomUUID(),
        "application2", None, collaborators, DateTime.now(), ApplicationState()),
      ApplicationResponse(UUID.randomUUID(),
        "application3", None, collaborators, DateTime.now(), ApplicationState()))
  }

  "fetchAllSubscribedApplications" should {

    "list all subscribed applications" in new Setup {
      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApplications))

      val subscriptions =
        Seq(SubscriptionResponse(APIIdentifier("test-context", "1.0"), Seq(allApplications.tail.head.id.toString)),
          SubscriptionResponse(APIIdentifier("unknown-context", "1.0"), Seq()),
          SubscriptionResponse(APIIdentifier("super-context", "1.0"), allApplications.map(_.id.toString)))


      given(testApplicationService.applicationConnector.fetchAllSubscriptions()(any[HeaderCarrier]))
        .willReturn(Future.successful(subscriptions))


      val result: Seq[SubscribedApplicationResponse] = await(testApplicationService.fetchAllSubscribedApplications)

      val app1 = result.find(sa => sa.name == "application1").get
      val app2 = result.find(sa => sa.name == "application2").get
      val app3 = result.find(sa => sa.name == "application3").get

      app1.subscriptionNames should have size 1
      app1.subscriptionNames shouldBe Seq("super-context")

      app2.subscriptionNames should have size 2
      app2.subscriptionNames shouldBe Seq("super-context", "test-context")

      app3.subscriptionNames should have size 1
      app3.subscriptionNames shouldBe Seq("super-context")
    }
  }

  "resendVerification" should {

    "call applicationConnector with appropriate parameters" in new Setup {
      val applicationId = "applicationId"
      val userName = "userName"
      val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
      val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testApplicationService.applicationConnector.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
        .willReturn(Future.successful(ResendVerificationSuccessful))

      val result = await(testApplicationService.resendVerification(applicationId, userName))

      appIdCaptor.getValue shouldBe applicationId
      gatekeeperIdCaptor.getValue shouldBe userName
    }
  }

  "fetchApplications" should {

    "list all applications when filtering not provided" in new Setup {
      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApplications))

      val result: Seq[ApplicationResponse] = await(testApplicationService.fetchApplications)
      result shouldEqual allApplications
    }

    "list filtered applications when specific subscription filtering is provided" in new Setup {
      val filteredApplications = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testApplicationService.applicationConnector.fetchAllApplicationsBySubscription(mEq("subscription"))(any[HeaderCarrier]))
        .willReturn(Future.successful(filteredApplications))

      val result = await(testApplicationService.fetchApplications(Value("subscription")))
      result shouldBe filteredApplications
    }

    "list filtered applications when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      val subscriptions = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application2", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application4", None, collaborators, DateTime.now(), ApplicationState()))

      val allApps = noSubscriptions ++ subscriptions
      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApps))
      given(testApplicationService.applicationConnector.fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier]))
        .willReturn(Future.successful(noSubscriptions))
      val result = await(testApplicationService.fetchApplications(OneOrMoreSubscriptions))
      result shouldBe subscriptions
    }

    "list filtered applications when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier]))
        .willReturn(Future.successful(allApps))
      val result = await(testApplicationService.fetchApplications(OneOrMoreApplications))
      result shouldBe allApps
    }
  }

  "updateRateLimitTier" should {

    "return successfully when updating the rate limit tier" in new Setup {
      val applicationId = UUID.randomUUID().toString
      val rateLimitTier = "SILVER"

      given(testApplicationService.applicationConnector.updateRateLimitTier(applicationId, rateLimitTier)(hc))
        .willReturn(Future.successful(UpdateApplicationRateLimitTierSuccessful))

      val result = await(testApplicationService.updateRateLimitTier(applicationId, rateLimitTier))
      result shouldBe UpdateApplicationRateLimitTierSuccessful
    }

    "propagate the error when trying to update rate limit tier" in new Setup {
      val applicationId = UUID.randomUUID().toString
      val rateLimitTier = "WOOD"

      given(testApplicationService.applicationConnector.updateRateLimitTier(applicationId, rateLimitTier)(hc))
        .willReturn(Future.failed(new RuntimeException))

      intercept[RuntimeException] {
        await(testApplicationService.updateRateLimitTier(applicationId, rateLimitTier))
      }
    }
  }

}
