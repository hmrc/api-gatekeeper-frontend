/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy orequest the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS Orequest ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import controllers.ControllerSetupBase
import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}
import model.{APIStatus, APIVersion, Subscription, VersionSubscription}
import org.mockito.BDDMockito.`given`
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsEmpty, Request, Result, Results}
import play.api.test.FakeRequest
import play.api.http.Status.OK
import services.ApplicationService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActionBuildersSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  trait Setup extends ControllerSetupBase {

    implicit val materializer = fakeApplication.materializer
    implicit val appConfig = mock[config.AppConfig]

    val underTest = new ActionBuilders {
      override val applicationService: ApplicationService = mockApplicationService
    }

    val ec = global
    val actionReturns200Body: Request[_] => HeaderCarrier => Future[Result] = _ => _ => Future.successful(Results.Ok)
    val aUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("username"), Enrolments(Set(Enrolment(userRole))), FakeRequest())

    val expectedResult = "result text"
    val version1 = VersionSubscription(APIVersion("version_1.0", APIStatus.STABLE, None), subscribed = true)
    val version2 = VersionSubscription(APIVersion("version_2.0", APIStatus.STABLE, None), subscribed = false)

    val emptySubscriptionFieldsWrapper = SubscriptionFieldsWrapper(applicationId, "clientId", "context", "apiVersion", fields = Seq.empty)
    val versionWithoutSubscriptionFields = VersionSubscription(APIVersion("version_3.0", APIStatus.STABLE, None), subscribed = true, fields = Some(emptySubscriptionFieldsWrapper))

    val subscriptionFieldDefinition = SubscriptionFieldDefinition("name", "description", "hint", "STRING")
    val subscriptionFieldValue = SubscriptionFieldValue(subscriptionFieldDefinition, "value")
    val subscriptionFieldsWrapper = emptySubscriptionFieldsWrapper.copy(fields = Seq(subscriptionFieldValue))
    val versionWithSubscriptionFields = VersionSubscription(APIVersion("version_4.0", APIStatus.STABLE, None), subscribed = true, fields = Some(subscriptionFieldsWrapper))

     val subscription1 = Subscription(name = "Subscription1",
      serviceName = "service name",
      context =  "context",
      versions = Seq.empty[VersionSubscription])

    val subscription2 = Subscription(name = "Subscription2",
      serviceName = "service name2",
      context =  "context2",
      versions = Seq(version1, version2))

    val subscription3 = Subscription(name = "Subscription3",
      serviceName = "service name3",
      context =  "context3",
      versions = Seq(version1, version2))

    val subscription4 = Subscription(name = "Subscription4",
      serviceName = "service name4",
      context =  "context4",
      versions = Seq(version1, versionWithoutSubscriptionFields, versionWithSubscriptionFields))
  }

  "withApp" should {
    "fetch application" in new Setup {

      fetchApplicationReturns(application)

      val result: Result = await(underTest.withApp(applicationId)(request => {
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec))

      verifyFetchApplication(applicationId)

      status(result) shouldBe OK
      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndSubscriptions" should {
    "fetch Subscriptions" when {
      "withSubFields is true" in new Setup {

        fetchApplicationReturns(application)
        fetchApplicationSubscriptionsReturns(Seq(subscription1, subscription2))

        val result: Result = await(underTest.withAppAndSubscriptions(applicationId, true)(request => {
          request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1)))
          Future.successful(Ok(expectedResult))
        })(aUserLoggedInRequest, ec))

        verifyFetchApplication(applicationId)
        verifyFetchApplicationSubscriptions(basicApplication, true)

        status(result) shouldBe OK
        bodyOf(result) shouldBe expectedResult
      }
      "withSubFields is false" in new Setup {
        fetchApplicationReturns(application)
        fetchApplicationSubscriptionsReturns(Seq(subscription1, subscription2))

        val result: Result = await(underTest.withAppAndSubscriptions(applicationId, false)(request => {
          Future.successful(Ok(""))
        })(aUserLoggedInRequest, ec))

        verifyFetchApplicationSubscriptions(basicApplication, false)
      }
    }

    "fetch sorted Subscriptions when withSubFields is true" in new Setup {

      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription3, subscription2))

      val result: Result = await(underTest.withAppAndSubscriptions(applicationId, true)(request => {
        request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1)), subscription3.copy(versions = Seq(version1)))
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec))

      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndFieldDefinitions" should {
    "fetch Subscriptions with Subscription Fields" in new Setup {

      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription4))

      val result: Result = await(underTest.withAppAndFieldDefinitions(applicationId)(request => {
        request.subscriptionsWithFieldDefinitions shouldBe Seq(subscription4.copy(versions = Seq(versionWithSubscriptionFields)))
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec))

      bodyOf(result) shouldBe expectedResult
    }
  }
}