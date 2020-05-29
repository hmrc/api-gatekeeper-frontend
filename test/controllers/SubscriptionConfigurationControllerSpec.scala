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

package controllers

import builder.SubscriptionsBuilder
import org.mockito.BDDMockito.`given`
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers._
import services.SubscriptionFieldsService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import utils.{TitleChecker, WithCSRFAddToken}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SubscriptionConfigurationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken with TitleChecker {
  implicit val materializer = fakeApplication.materializer

  trait Setup extends ControllerSetupBase with SubscriptionsBuilder  {
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val controller = new SubscriptionConfigurationController(
      mockApplicationService,
      mockAuthConnector
    )(mockConfig, global)

    val version = "1.0"
    val context = "my-context"
    val subscriptionFieldValue = buildSubscriptionFieldValue("name")
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, Seq(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(version, true, fields = Some(subscriptionFieldsWrapper))
    val subscription = buildSubscription("My Subscription", Some(context), Seq(versionWithSubscriptionFields))

  }

  "list Subscription Configuration" should {
    "show subscriptions configuration" in new Setup {
      givenTheUserIsAuthorisedAndIsANormalUser()
      givenTheAppWillBeReturned()
      given(mockConfig.title).willReturn("Unit Test Title")
      given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))((any[HeaderCarrier])))
        .willReturn(Future.successful(Seq(subscription)))

      val result : Result = await(controller.listConfigurations(applicationId)(aLoggedInRequest))

      status(result) shouldBe OK

      titleOf(result) shouldBe "Unit Test Title - Subscription configuration"

      val responseBody = Helpers.contentAsString(result)
      responseBody should include("<h1>Subscription configuration</h1>")
      responseBody should include(subscription.name)
      responseBody should include(subscription.versions.head.version.version)
      responseBody should include(subscription.versions.head.version.displayedStatus)
      responseBody should include(subscription.versions.head.fields.head.fields.head.definition.shortDescription)
      responseBody should include(subscription.versions.head.fields.head.fields.head.value)

      verify(mockApplicationService).fetchApplication(eqTo(applicationId))(any[HeaderCarrier])
    }

    "When logged in as super user renders the page correctly" in new Setup {
      givenTheUserIsAuthorisedAndIsASuperUser()
      givenTheAppWillBeReturned()
      given(mockConfig.title).willReturn("Unit Test Title")
      given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))((any[HeaderCarrier])))
        .willReturn(Future.successful(Seq(subscription)))

      val result : Result = await(controller.listConfigurations(applicationId)(aSuperUserLoggedInRequest))

      status(result) shouldBe OK

      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new Setup {
      givenTheUserHasInsufficientEnrolments()

      val result : Result = await(controller.listConfigurations(applicationId)(aLoggedInRequest))
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    } 
  }

  "edit Subscription Configuration" should {
    "show Subscription Configuration" in new Setup {
      givenTheUserIsAuthorisedAndIsANormalUser()
      givenTheAppWillBeReturned()
      given(mockConfig.title).willReturn("Unit Test Title")
      given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))((any[HeaderCarrier])))
        .willReturn(Future.successful(Seq(subscription)))

      val result : Result = await(controller.editConfigurations(applicationId, context, version)(aLoggedInRequest))

      status(result) shouldBe OK

      titleOf(result) shouldBe s"${mockConfig.title} - ${subscription.name} $version ${subscription.versions.head.version.displayedStatus}"

      val responseBody = Helpers.contentAsString(result)

      // TODO: Check edit stuff is displayed.

      verify(mockApplicationService).fetchApplication(eqTo(applicationId))(any[HeaderCarrier])
    }
    
    "bad requst for invalud context or version" in new Setup {
      
    }

    "something to do with roles" in new Setup {

    }
  }
}
