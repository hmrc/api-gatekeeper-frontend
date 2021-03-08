/*
 * Copyright 2021 HM Revenue & Customs
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
import mocks.services.SubscriptionFieldsServiceMock
import model.{ApiContext, ApiVersion, FieldName, FieldValue}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.applications.subscriptionConfiguration.{EditSubscriptionConfigurationView, ListSubscriptionConfigurationView}
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import builder.{ApiBuilder, SubscriptionsBuilder, ApplicationBuilder}
import builder.FieldDefinitionsBuilder
import model.ApiStatus
import model.Environment

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
      errorTemplateView,
      mockApmService
    )

    val version = ApiVersion.random
    val context = ApiContext.random
    val subscriptionFieldValue = buildSubscriptionFieldValue(FieldName.random)
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, List(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(version, true, applicationId, fields = Some(subscriptionFieldsWrapper))
    val subscription = buildSubscription("My Subscription", Some(context), List(versionWithSubscriptionFields))
  }

  trait AppWithSubscriptionDataAndFieldDefinitionsSetup extends Setup with FieldDefinitionsBuilder with ApiBuilder with ApplicationBuilder {
    val allFieldDefinitions = buildApiDefinitions()

    val apiContext = allFieldDefinitions.keySet.head
    val apiVersion = allFieldDefinitions(apiContext).keySet.head

    val fields = allFieldDefinitions(apiContext)(apiVersion).map(f => f._1 -> FieldValue.random)

    val applicationWithSubscriptionData = buildApplicationWithSubscriptionData(apiContext, apiVersion, fields)

    val apiName = "API Name"
    val versionData = DefaultVersionData
    val apiData = DefaultApiData.withName(apiName).withVersion(apiVersion, versionData)
    val allPossibleSubs = Map(apiContext -> apiData)
  }

  trait EditSaveFormData extends AppWithSubscriptionDataAndFieldDefinitionsSetup {
    def requestWithFormData(fieldName: FieldName, fieldValue: FieldValue)(request : FakeRequest[_]) = {
      request.withFormUrlEncodedBody(
        "fields[0].name" -> fieldName.value,
        "fields[0].value" -> fieldValue.value)
    }
  }

  "list Subscription Configuration" should {
    "show subscriptions configuration" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      givenTheGKUserIsAuthorisedAndIsASuperUser()
      
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)
      
      val result = controller.listConfigurations(applicationId)(aLoggedInRequest)
      status(result) shouldBe OK

      titleOf(result) shouldBe "Unit Test Title - Subscription configuration"

      val responseBody = contentAsString(result)
      responseBody should include("<h1>Subscription configuration</h1>")
      responseBody should include(apiName)
      responseBody should include(apiVersion.value)
      responseBody should include(ApiStatus.displayedStatus(versionData.status))
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.shortDescription)
      responseBody should include(fields.head._2.value)

      verifyFetchApplicationById(applicationId)
      verifyAllPossibleSubscriptions(applicationId)
      verifyGetAllFieldDefinitionsReturns(Environment.SANDBOX)

    }

    "When logged in as super user renders the page correctly" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      givenTheGKUserIsAuthorisedAndIsASuperUser()

      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = controller.listConfigurations(applicationId)(aSuperUserLoggedInRequest)
      status(result) shouldBe OK
      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new Setup {
      givenTheGKUserHasInsufficientEnrolments()

      val result = controller.listConfigurations(applicationId)(aLoggedInRequest)
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    }
  }

  "edit Subscription Configuration" should {
    "show Subscription Configuration" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)
      
      val result = addToken(controller.editConfigurations(applicationId, apiContext, apiVersion))(aLoggedInRequest)

      status(result) shouldBe OK

      titleOf(result) shouldBe s"${appConfig.title} - ${apiName} ${apiVersion.value} ${ApiStatus.displayedStatus(versionData.status)}"

      val responseBody = Helpers.contentAsString(result)

      responseBody should include(apiName)
      responseBody should include(apiVersion.value)
      responseBody should include(ApiStatus.displayedStatus(versionData.status))
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.description)
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.hint)
      responseBody should include(fields.head._2.value)

      verifyFetchApplicationById(applicationId)
      verifyAllPossibleSubscriptions(applicationId)
      verifyGetAllFieldDefinitionsReturns(Environment.SANDBOX)
    }

     "When logged in as super user renders the page correctly" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      givenTheGKUserIsAuthorisedAndIsASuperUser()
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = addToken(controller.editConfigurations(applicationId, apiContext, apiVersion))(aSuperUserLoggedInRequest)

      status(result) shouldBe OK

      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new Setup {
      givenTheGKUserHasInsufficientEnrolments()

      val result = controller.editConfigurations(applicationId, context, version)(aLoggedInRequest)
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    }
  }

  "save subscription configuration post" should {
    "save" in new EditSaveFormData {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)
      givenSaveSubscriptionFieldsSuccess

      val newValue = FieldValue.random

      val request = requestWithFormData(fields.head._1 , newValue)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, apiContext, apiVersion))(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/subscriptions-configuration")
    
      val expectedFields = Map(fields.head._1 -> newValue)

      verifySaveSubscriptionFields(applicationWithSubscriptionData.application, apiContext, apiVersion, expectedFields)
    }

    "save gives validation errors" in new EditSaveFormData {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)
      
      val validationMessage = "My validation error"
      val errors = Map(fields.head._1.value -> validationMessage)

      givenSaveSubscriptionFieldsFailure(errors)

      val request = requestWithFormData(fields.head._1 , FieldValue.random)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, apiContext, apiVersion))(request)

      status(result) shouldBe BAD_REQUEST

      val responseBody = Helpers.contentAsString(result)
      responseBody should include(validationMessage)
    }

    "When logged in as super saves the data" in new EditSaveFormData {
      givenTheGKUserIsAuthorisedAndIsASuperUser()
      FetchApplicationById.returns(applicationWithSubscriptionData)
      getAllFieldDefinitionsReturns(allFieldDefinitions)
      fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      // givenTheSubscriptionsWillBeReturned(application.application, true, Seq(subscription))
      givenSaveSubscriptionFieldsSuccess

      val request = requestWithFormData(FieldName.random, FieldValue.empty)(aSuperUserLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, context, version))(request)

      status(result) shouldBe SEE_OTHER
      verifyAuthConnectorCalledForSuperUser
    }

    "When logged in as normal user renders forbidden page" in new EditSaveFormData {
      givenTheGKUserHasInsufficientEnrolments()
      
      val request = requestWithFormData(FieldName.random, FieldValue.empty)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, context, version))(request)
      status(result) shouldBe FORBIDDEN
      verifyAuthConnectorCalledForSuperUser
    }
  }
}
