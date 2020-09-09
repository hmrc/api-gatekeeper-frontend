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
import mocks.service.SubscriptionFieldsServiceMock
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.http.HeaderCarrier
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.applications.subscriptionConfiguration.{EditSubscriptionConfigurationView, ListSubscriptionConfigurationView}
import views.html.{ErrorTemplate, ForbiddenView}
import model.ApiContext

import scala.concurrent.ExecutionContext.Implicits.global

class SubscriptionConfigurationControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker {

  implicit val materializer = app.materializer
  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val listSubscriptionConfigurationView = app.injector.instanceOf[ListSubscriptionConfigurationView]
  private lazy val editSubscriptionConfigurationView = app.injector.instanceOf[EditSubscriptionConfigurationView]

  trait Setup extends ControllerSetupBase with SubscriptionsBuilder with SubscriptionFieldsServiceMock {
    lazy val controller = new SubscriptionConfigurationController (
      mockApplicationService,
      mockSubscriptionFieldsService,
      mockAuthConnector,
      forbiddenView,
      mcc,
      listSubscriptionConfigurationView,
      editSubscriptionConfigurationView,
      errorTemplateView
    )

    val version = "1.0"
    val context = ApiContext.random
    val subscriptionFieldValue = buildSubscriptionFieldValue("field-name")
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, Seq(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(version, true, applicationId, fields = Some(subscriptionFieldsWrapper))
    val subscription = buildSubscription("My Subscription", Some(context), Seq(versionWithSubscriptionFields))
  }

  "list Subscription Configuration" should {
    "show subscriptions configuration" in new Setup {
      givenTheUserIsAuthorisedAndIsANormalUser()
      givenTheAppWillBeReturned()
      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))

      val result : Result = await(controller.listConfigurations(applicationId)(aLoggedInRequest))
      status(result) shouldBe OK

      titleOf(result) shouldBe "Unit Test Title - Subscription configuration"

      val responseBody = Helpers.contentAsString(result)
      responseBody should include("<h1>Subscription configuration</h1>")
      responseBody should include(subscription.name)
      responseBody should include(subscription.versions.head.version.version)
      responseBody should include(subscription.versions.head.version.displayedStatus)
      responseBody should include(subscription.versions.head.fields.fields.head.definition.shortDescription)
      responseBody should include(subscription.versions.head.fields.fields.head.value)

      verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
    }

    "When logged in as super user renders the page correctly" in new Setup {
      givenTheUserIsAuthorisedAndIsASuperUser()
      givenTheAppWillBeReturned()
      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))

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
      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))

      val result : Result = await(addToken(controller.editConfigurations(applicationId, context, version))(aLoggedInRequest))

      status(result) shouldBe OK

      titleOf(result) shouldBe s"${appConfig.title} - ${subscription.name} $version ${subscription.versions.head.version.displayedStatus}"

      val responseBody = Helpers.contentAsString(result)

      responseBody should include(subscription.name)
      responseBody should include(subscription.versions.head.version.version)
      responseBody should include(subscription.versions.head.version.displayedStatus)
      
      responseBody should include(subscriptionFieldValue.definition.description)
      responseBody should include(subscriptionFieldValue.definition.hint)
      responseBody should include(subscriptionFieldValue.value)
    
      verify(mockApplicationService).fetchApplication(eqTo(applicationId))(*)
    }

     "When logged in as super user renders the page correctly" in new Setup {
      givenTheUserIsAuthorisedAndIsASuperUser()
      givenTheAppWillBeReturned()
      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))

      val result : Result = await(addToken(controller.editConfigurations(applicationId, context, version))(aSuperUserLoggedInRequest))

      status(result) shouldBe OK

      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new Setup {
      givenTheUserHasInsufficientEnrolments()

      val result : Result = await(controller.editConfigurations(applicationId, context, version)(aLoggedInRequest))
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    }
  }

  "save subscription configuration post" should {
    "save" in new EditSaveFormData {
      givenTheUserIsAuthorisedAndIsANormalUser()
      givenTheAppWillBeReturned()
      
      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))
      givenSaveSubscriptionFieldsSuccess

      val newValue = "new value"

      val request = requestWithFormData(subscriptionFieldValue.definition.name, newValue)(aLoggedInRequest)

      val result = await(addToken(controller.saveConfigurations(applicationId, context, version))(request))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions-configuration")
    
      val expectedFields = Map(subscriptionFieldValue.definition.name -> newValue)

      verifySaveSubscriptionFields(application.application, context, version, expectedFields)
    }

    "save gives validation errors" in new EditSaveFormData {
      givenTheUserIsAuthorisedAndIsANormalUser()
      givenTheAppWillBeReturned()
      
      val validationMessage = "My validation error"
      val errors = Map(subscriptionFieldValue.definition.name -> validationMessage)

      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))
      givenSaveSubscriptionFieldsFailure(errors)

      val request = requestWithFormData(subscriptionFieldValue.definition.name , "field value")(aLoggedInRequest)

      val result = await(addToken(controller.saveConfigurations(applicationId, context, version))(request))

      status(result) shouldBe BAD_REQUEST

      val responseBody = Helpers.contentAsString(result)
      responseBody should include(validationMessage)
    }

    "When logged in as super saves the data" in new EditSaveFormData {
      givenTheUserIsAuthorisedAndIsASuperUser()
      givenTheAppWillBeReturned()

      givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))
      givenSaveSubscriptionFieldsSuccess

      val request = requestWithFormData("", "")(aSuperUserLoggedInRequest)

      val result = await(addToken(controller.saveConfigurations(applicationId, context, version))(request))

      status(result) shouldBe SEE_OTHER
      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new EditSaveFormData {
      givenTheUserHasInsufficientEnrolments()
      
      val request = requestWithFormData("","")(aLoggedInRequest)

      val result = await(addToken(controller.saveConfigurations(applicationId, context, version))(request))
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    }
  }

  trait EditSaveFormData extends Setup {
    def requestWithFormData(fieldName: String, fieldValue: String)(request : FakeRequest[_]) = {
      request.withFormUrlEncodedBody(
        "fields[0].name" -> fieldName,
        "fields[0].value" -> fieldValue)
    }
  }
}
