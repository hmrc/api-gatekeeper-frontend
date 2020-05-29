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
import play.api.http.Status.{OK, NOT_FOUND}
import services.ApplicationService
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import scala.concurrent.Future

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.Messages.Implicits.applicationMessages
import org.scalatestplus.play.guice.GuiceFakeApplicationFactory
import model.ApplicationWithHistory

class ActionBuildersSpec extends UnitSpec with MockitoSugar with WithFakeApplication with SubscriptionsBuilder {

  trait Setup extends ControllerSetupBase {
    val underTest = new ActionBuilders {
      override val applicationService: ApplicationService = mockApplicationService
    }

    implicit val materializer = fakeApplication.materializer

    implicit var playApplication = fakeApplication

    implicit val appConfig = mock[config.AppConfig]
    
    implicit val messages: play.api.i18n.Messages = applicationMessages
    
    val ec = global
    val actionReturns200Body: Request[_] => HeaderCarrier => Future[Result] = _ => _ => Future.successful(Results.Ok)
    val aUserLoggedInRequest = LoggedInRequest[AnyContentAsEmpty.type](Some("username"), Enrolments(Set(Enrolment(userRole))), FakeRequest())

    val expectedResult = "result text"
  }

  trait SubscriptionsWithMixOfSubscribedVersionsSetup extends Setup {
    val version1Subscribed = buildVersionWithSubscriptionFields("1.0", true)
    val version2NotSubscribed = buildVersionWithSubscriptionFields("2.0", false)

    val emptySubscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId)
    val versionWithoutSubscriptionFields = buildVersionWithSubscriptionFields("3.0", true, fields = Some(emptySubscriptionFieldsWrapper))

    val subscriptionFieldValue = buildSubscriptionFieldValue("name")
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, Seq(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields("4.0", true, fields = Some(subscriptionFieldsWrapper))

     val subscription1 = buildSubscription("Subscription1")

    val subscription2 = buildSubscription("Subscription2", versions = Seq(version1Subscribed, version2NotSubscribed))

    val subscription3 = buildSubscription("Subscription3", versions = Seq(version1Subscribed, version2NotSubscribed))

    val subscription4 = buildSubscription("Subscription4", versions = Seq(version1Subscribed, versionWithoutSubscriptionFields, versionWithSubscriptionFields))
  }

  "withApp" should {
    "fetch application" in new SubscriptionsWithMixOfSubscribedVersionsSetup {

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
      "withSubFields is true" in new SubscriptionsWithMixOfSubscribedVersionsSetup {

        fetchApplicationReturns(application)
        fetchApplicationSubscriptionsReturns(Seq(subscription1, subscription2))

        val result: Result = await(underTest.withAppAndSubscriptions(applicationId, true)(request => {
          request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1Subscribed)))
          Future.successful(Ok(expectedResult))
        })(aUserLoggedInRequest, ec))

        verifyFetchApplication(applicationId)
        verifyFetchApplicationSubscriptions(basicApplication, true)

        status(result) shouldBe OK
        bodyOf(result) shouldBe expectedResult
      }
      "withSubFields is false" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
        fetchApplicationReturns(application)
        fetchApplicationSubscriptionsReturns(Seq(subscription1, subscription2))

        val result: Result = await(underTest.withAppAndSubscriptions(applicationId, false)(request => {
          Future.successful(Ok(""))
        })(aUserLoggedInRequest, ec))

        verifyFetchApplicationSubscriptions(basicApplication, false)
      }
    }

    "fetch sorted Subscriptions when withSubFields is true" in new SubscriptionsWithMixOfSubscribedVersionsSetup {

      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription3, subscription2))

      val result: Result = await(underTest.withAppAndSubscriptions(applicationId, true)(request => {
        request.subscriptions shouldBe Seq(subscription2.copy(versions = Seq(version1Subscribed)), subscription3.copy(versions = Seq(version1Subscribed)))
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec))

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
      })(aUserLoggedInRequest, ec))

      bodyOf(result) shouldBe expectedResult
    }
  }

  "withAppAndSubscriptionVersion" should {
    val subscription = buildSubscription("mySubscription", versions = Seq(
      buildVersionWithSubscriptionFields("1.0", true),
      buildVersionWithSubscriptionFields("2.0", true)
    ))

    "fetch subscription and version" in new Setup {
      val version = subscription.versions.head
      val context = subscription.context
    
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))
  
      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, context, version.version.version)(request => {
        request.subscription shouldBe subscription
        request.version shouldBe version
        
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec, messages ,appConfig))

      bodyOf(result) shouldBe expectedResult
    }

    "404 for invalid context" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      val version = subscription.versions.head

      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))

      val invalidContext = "not-a-context"

      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, invalidContext, version.version.version)(request => {
        throw new RuntimeException("This shouldn't be called")  
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec, messages ,appConfig))

      status(result) shouldBe NOT_FOUND
    }

    "404 for invalid version" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      fetchApplicationReturns(application)
      fetchApplicationSubscriptionsReturns(Seq(subscription))

      val context = subscription.context
      val invalidVersion = "not-a-version"

      val result: Result = await(underTest.withAppAndSubscriptionVersion(applicationId, context, invalidVersion)(request => {
        throw new RuntimeException("This shouldn't be called")  
        Future.successful(Ok(expectedResult))
      })(aUserLoggedInRequest, ec, messages ,appConfig))

      status(result) shouldBe NOT_FOUND
    }
  }
}