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
import model.SubscriptionFields.Fields
import play.mvc.Http
import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpResponse

class SubscriptionConfigurationControllerSpec 
    extends UnitSpec 
    with MockitoSugar 
    with WithFakeApplication 
    with WithCSRFAddToken 
    with TitleChecker {
  implicit val materializer = fakeApplication.materializer

  trait Setup extends ControllerSetupBase with SubscriptionsBuilder  {
    val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

    val controller = new SubscriptionConfigurationController(
      mockApplicationService,
      mockSubscriptionFieldsService,
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

      val result : Result = await(addToken(controller.editConfigurations(applicationId, context, version))(aLoggedInRequest))

      status(result) shouldBe OK

      titleOf(result) shouldBe s"${mockConfig.title} - ${subscription.name} $version ${subscription.versions.head.version.displayedStatus}"

      val responseBody = Helpers.contentAsString(result)

      responseBody should include(subscription.name)
      responseBody should include(subscription.versions.head.version.version)
      responseBody should include(subscription.versions.head.version.displayedStatus)
      
      responseBody should include(subscriptionFieldValue.definition.description)      
      responseBody should include(subscriptionFieldValue.definition.hint)
      responseBody should include(subscriptionFieldValue.value)
    
      verify(mockApplicationService).fetchApplication(eqTo(applicationId))(any[HeaderCarrier])
    }

    // TODO
    "something to do with roles" in new Setup {

    }
  }

  "save subscription configuration post" should {
    "save when valid" in new Setup {
      givenTheUserIsAuthorisedAndIsANormalUser()  
      givenTheAppWillBeReturned()
      
      given(mockApplicationService.fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))((any[HeaderCarrier])))
         .willReturn(Future.successful(Seq(subscription)))

      val httpResponse = mock[HttpResponse]
      given(mockSubscriptionFieldsService.saveFieldValues(any(), any(), any(), any())(any[HeaderCarrier]))
        .willReturn(Future.successful(httpResponse))

      val fieldName = "fieldName"
      val fieldValue = "new value"
    
      val requestWithFormData = aLoggedInRequest.withFormUrlEncodedBody(
        "fields[0].name" -> fieldName,
        "fields[0].value" -> fieldValue)

      val result = await(addToken(controller.saveConfigurations(applicationId, context, version))(requestWithFormData))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/subscriptions-configuration")
    
      val expectedFields = Map(fieldName -> fieldValue)
      verify(mockSubscriptionFieldsService).saveFieldValues(eqTo(application.application), eqTo(context), eqTo(version), eqTo(expectedFields))(any[HeaderCarrier])
    }

    // TODO?
    "Invalid form? Is this possible?" in new Setup {

    }

    // TODO
    "Invalid returned from service" in new Setup {

    }

    // TODO
    "Something to do with roles" in new Setup {

    }
  }
}
