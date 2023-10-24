/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import mocks.services.SubscriptionFieldsServiceMockProvider

import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder, FieldDefinitionsBuilder, SubscriptionsBuilder}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models.{FieldName, FieldValue}
import uk.gov.hmrc.gatekeeper.utils.{TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.applications.subscriptionConfiguration.{EditSubscriptionConfigurationView, ListSubscriptionConfigurationView}
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class SubscriptionConfigurationControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker {

  implicit val materializer                          = app.materializer
  private lazy val errorTemplateView                 = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                     = app.injector.instanceOf[ForbiddenView]
  private lazy val listSubscriptionConfigurationView = app.injector.instanceOf[ListSubscriptionConfigurationView]
  private lazy val editSubscriptionConfigurationView = app.injector.instanceOf[EditSubscriptionConfigurationView]
  private lazy val errorHandler                      = app.injector.instanceOf[ErrorHandler]

  trait Setup extends ControllerSetupBase
      with SubscriptionsBuilder
      with SubscriptionFieldsServiceMockProvider {

    lazy val controller = new SubscriptionConfigurationController(
      mockApplicationService,
      mockSubscriptionFieldsService,
      forbiddenView,
      mcc,
      listSubscriptionConfigurationView,
      editSubscriptionConfigurationView,
      errorTemplateView,
      mockApmService,
      errorHandler,
      StrideAuthorisationServiceMock.aMock
    )

    val version                       = ApiVersionNbr.random
    val context                       = ApiContext.random
    val subscriptionFieldValue        = buildSubscriptionFieldValue(FieldName.random)
    val subscriptionFieldsWrapper     = buildSubscriptionFieldsWrapper(applicationId, List(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(version, true, applicationId, fields = Some(subscriptionFieldsWrapper))
    val subscription                  = buildSubscription("My Subscription", Some(context), List(versionWithSubscriptionFields))
  }

  trait AppWithSubscriptionDataAndFieldDefinitionsSetup extends Setup with FieldDefinitionsBuilder with ApiBuilder with ApplicationBuilder {
    val allFieldDefinitions = buildApiDefinitionFields()

    val apiContext = allFieldDefinitions.keySet.head
    val apiVersion = allFieldDefinitions(apiContext).keySet.head

    val fields = allFieldDefinitions(apiContext)(apiVersion).map(f => f._1 -> FieldValue.random)

    val applicationWithSubscriptionData = buildApplicationWithSubscriptionData(apiContext, apiVersion, fields)

    val apiName         = "API Name"
    val versionData     = DefaultVersionData
    val apiDefinition   = DefaultApiDefinition.withName(apiName).withVersion(apiVersion, versionData).withContext(apiContext)
    val allPossibleSubs = List(apiDefinition)
  }

  trait EditSaveFormData extends AppWithSubscriptionDataAndFieldDefinitionsSetup {

    def requestWithFormData(fieldName: FieldName, fieldValue: FieldValue)(request: FakeRequest[_]) = {
      request.withFormUrlEncodedBody(
        "fields[0].name"  -> fieldName.value,
        "fields[0].value" -> fieldValue.value
      )
    }
  }

  "list Subscription Configuration" should {
    "show subscriptions configuration" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = controller.listConfigurations(applicationId)(aLoggedInRequest)
      status(result) shouldBe OK

      titleOf(result) shouldBe "Unit Test Title - Subscription configuration"

      val responseBody = contentAsString(result)
      responseBody should include("Subscription configuration")
      responseBody should include(apiName)
      responseBody should include(apiVersion.value)
      responseBody should include(versionData.status.displayText)
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.shortDescription)
      responseBody should include(fields.head._2.value)

      ApmServiceMock.verifyFetchApplicationById(applicationId)
      ApmServiceMock.verifyAllPossibleSubscriptions(applicationId)
      ApmServiceMock.verifyGetAllFieldDefinitionsReturns(Environment.SANDBOX)

    }

    "When logged in as super user renders the page correctly" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = controller.listConfigurations(applicationId)(aSuperUserLoggedInRequest)
      status(result) shouldBe OK
    }

    "When logged in as normal user renders forbidden page" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

      val result = controller.listConfigurations(applicationId)(aLoggedInRequest)
      status(result) shouldBe FORBIDDEN
    }
  }

  "edit Subscription Configuration" should {
    "show Subscription Configuration" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = addToken(controller.editConfigurations(applicationId, apiContext, apiVersion))(aLoggedInRequest)

      status(result) shouldBe OK

      titleOf(result) shouldBe s"${appConfig.title} - ${apiName} ${apiVersion.value} ${versionData.status.displayText}"

      val responseBody = Helpers.contentAsString(result)

      responseBody should include(apiName)
      responseBody should include(apiVersion.value)
      responseBody should include(versionData.status.displayText)
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.description)
      responseBody should include(allFieldDefinitions(apiContext)(apiVersion).head._2.hint)
      responseBody should include(fields.head._2.value)

      ApmServiceMock.verifyFetchApplicationById(applicationId)
      ApmServiceMock.verifyAllPossibleSubscriptions(applicationId)
      ApmServiceMock.verifyGetAllFieldDefinitionsReturns(Environment.SANDBOX)
    }

    "When logged in as super user renders the page correctly" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val result = addToken(controller.editConfigurations(applicationId, apiContext, apiVersion))(aSuperUserLoggedInRequest)

      status(result) shouldBe OK

    }

    "When logged in as normal user renders forbidden page" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

      val result = controller.editConfigurations(applicationId, context, version)(aLoggedInRequest)
      status(result) shouldBe FORBIDDEN
    }
  }

  "save subscription configuration post" should {
    "save" in new EditSaveFormData {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)
      SubscriptionFieldsServiceMock.SaveFieldValues.succeeds()

      val newValue = FieldValue.random

      val request = requestWithFormData(fields.head._1, newValue)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, apiContext, apiVersion))(request)

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/subscriptions-configuration")

      val expectedFields = Map(fields.head._1 -> newValue)

      SubscriptionFieldsServiceMock.SaveFieldValues.verifyParams(applicationWithSubscriptionData.application, apiContext, apiVersion, expectedFields)
    }

    "save gives validation errors" in new EditSaveFormData {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      val validationMessage = "My validation error"
      val errors            = Map(fields.head._1.value -> validationMessage)

      SubscriptionFieldsServiceMock.SaveFieldValues.failsWithFieldErrors(errors)

      val request = requestWithFormData(fields.head._1, FieldValue.random)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, apiContext, apiVersion))(request)

      status(result) shouldBe BAD_REQUEST

      val responseBody = Helpers.contentAsString(result)
      responseBody should include(validationMessage)
    }

    "When logged in as super saves the data" in new EditSaveFormData {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(allPossibleSubs)

      SubscriptionFieldsServiceMock.SaveFieldValues.succeeds()

      val request = requestWithFormData(FieldName.random, FieldValue.empty)(aSuperUserLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, context, version))(request)

      status(result) shouldBe SEE_OTHER
    }

    "When logged in as normal user renders forbidden page" in new EditSaveFormData {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

      val request = requestWithFormData(FieldName.random, FieldValue.empty)(aLoggedInRequest)

      val result = addToken(controller.saveConfigurations(applicationId, context, version))(request)
      status(result) shouldBe FORBIDDEN
    }
  }
}
