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

package utils

import builder.SubscriptionsBuilder
import controllers.{ControllerBaseSpec, ControllerSetupBase}
import mocks.TestRoles
import model.{ApiContext, ApiVersion, LoggedInRequest}
import play.api.http.Status.{NOT_FOUND, OK}
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import services.ApplicationService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import views.html.ErrorTemplate

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActionBuildersSpec extends ControllerBaseSpec with SubscriptionsBuilder {
  trait Setup extends ControllerSetupBase {
    implicit val materializer = app.materializer
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new ActionBuilders {
      val applicationService: ApplicationService = mockApplicationService
      val errorTemplate: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
    }
    
    implicit val aUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("username"), Enrolments(Set(Enrolment(TestRoles.userRole))), FakeRequest())
    implicit val messages = mcc.messagesApi.preferred(aUserLoggedInRequest)

    val actionReturns200Body: Request[_] => HeaderCarrier => Future[Result] = _ => _ => Future.successful(Results.Ok)

    val expectedResult = "result text"
  }

  trait withSubscription extends Setup {
    def subscription = buildSubscription("mySubscription", versions = Seq(
      buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId),
      buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId)
    ))
  }
  
  trait SubscriptionsWithMixOfSubscribedVersionsSetup extends withSubscription {
    val version1Subscribed = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId)
    val version2NotSubscribed = buildVersionWithSubscriptionFields(ApiVersion.random, false, applicationId)

    val emptySubscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId)
    val versionWithoutSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId, fields = Some(emptySubscriptionFieldsWrapper))

    val subscriptionFieldValue = buildSubscriptionFieldValue("name")
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, Seq(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId, fields = Some(subscriptionFieldsWrapper))

    val subscription1 = buildSubscription("Subscription1")

    val subscription2 = buildSubscription("Subscription2", versions = Seq(version1Subscribed, version2NotSubscribed))

    val subscription3 = buildSubscription("Subscription3", versions = Seq(version1Subscribed, version2NotSubscribed))

    val subscription4 = buildSubscription("Subscription4", versions = Seq(version1Subscribed, versionWithoutSubscriptionFields, versionWithSubscriptionFields))
  }

  "withApp" should {
    "fetch application" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      fetchApplicationReturns(application)

      val result: Result = await(underTest.withApp(applicationId)(_ => {
        Future.successful(Ok(expectedResult))
      }))

      verifyFetchApplication(applicationId)

      status(result) shouldBe OK
      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndSubscriptions" should {
    "fetch Subscriptions" when {
      "withSubFields is true" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
        fetchApplicationReturns(application)
        fetchApplicationSubscriptionsReturns(Seq(subscription1, subscription2))

        val result: Result = await(underTest.withAppAndSubscriptions(applicationId)(request => {
          request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1Subscribed)))
          Future.successful(Ok(expectedResult))
        }))

        verifyFetchApplication(applicationId)
        verifyFetchApplicationSubscriptions(basicApplication, true)

        status(result) shouldBe OK
        bodyOf(result) shouldBe expectedResult
      }
    }

    "fetch sorted Subscriptions when withSubFields is true" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription3, subscription2))

      val result: Result = await(underTest.withAppAndSubscriptions(applicationId)(request => {
        request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1Subscribed)), subscription3.copy(versions = Seq(version1Subscribed)))
        Future.successful(Ok(expectedResult))
      }))

      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndFieldDefinitions" should {
    "fetch Subscriptions with Subscription Fields" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription4))

      val result: Result = await(underTest.withAppAndFieldDefinitions(applicationId)(request => {
        request.subscriptionsWithFieldDefinitions shouldBe Seq(subscription4.copy(versions = Seq(versionWithSubscriptionFields)))
        Future.successful(Ok(expectedResult))
      }))

      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndSubscriptionVersion" should {
    "fetch subscription and version" in new withSubscription {
      val version = subscription.versions.head
      val context = subscription.context
    
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))
  
      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, context, version.version.version)(request => {
        request.subscription shouldBe subscription
        request.version shouldBe version
        
        Future.successful(Ok(expectedResult))
      }))

      bodyOf(result) shouldBe expectedResult
    }

    "404 for invalid context" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      val version = subscription.versions.head

      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))

      val invalidContext = ApiContext("not-a-context")

      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, invalidContext, version.version.version)(_ => {
        throw new RuntimeException("This shouldn't be called")
        Future.successful(Ok(expectedResult))
      }))

      status(result) shouldBe NOT_FOUND
    }

    "404 for invalid version" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))

      val apiContext = subscription.context
      val invalidVersion = ApiVersion("not-a-version")

      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, apiContext, invalidVersion)(_ => {
        throw new RuntimeException("This shouldn't be called")
        Future.successful(Ok(expectedResult))
      }))

      status(result) shouldBe NOT_FOUND
    }
  }
}
