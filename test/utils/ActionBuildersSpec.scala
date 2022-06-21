/*
 * Copyright 2022 HM Revenue & Customs
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

import builder.{SubscriptionsBuilder, ApplicationBuilder, FieldDefinitionsBuilder, ApiBuilder}
import controllers.{ControllerBaseSpec, ControllerSetupBase}
import model.ApiVersion
import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest
import play.api.mvc.Results.Ok
import play.api.mvc._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.ApplicationService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import model.FieldName
import services.ApmService
import model.State
import controllers.actions.ActionBuilders
import play.api.mvc.MessagesRequest
import config.ErrorHandler
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles

class ActionBuildersSpec extends ControllerBaseSpec {
  trait Setup extends ControllerSetupBase {
    implicit val materializer = app.materializer
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val underTest = new ActionBuilders {
      val applicationService: ApplicationService = mockApplicationService
      val apmService: ApmService = mockApmService
      val errorHandler: ErrorHandler = app.injector.instanceOf[ErrorHandler]
    }

    val fakeRequest = FakeRequest()
    val msgRequest = new MessagesRequest(fakeRequest, stubMessagesApi())
    implicit val aUserLoggedInRequest = new LoggedInRequest[AnyContentAsEmpty.type](Some("username"), GatekeeperRoles.USER, msgRequest)
    implicit val messages = mcc.messagesApi.preferred(aUserLoggedInRequest)

    val actionReturns200Body: Request[_] => HeaderCarrier => Future[Result] = _ => _ => Future.successful(Results.Ok)

    val expectedResult = "result text"
  }

  trait WithSubscription extends Setup with SubscriptionsBuilder {
    val subscription = buildSubscription("mySubscription", versions = List(
      buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId),
      buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId)
    ))
  }
  
  trait SubscriptionsWithMixOfSubscribedVersionsSetup extends WithSubscription {
    val version1Subscribed = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId)
    val version2NotSubscribed = buildVersionWithSubscriptionFields(ApiVersion.random, false, applicationId)

    val emptySubscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId)
    val versionWithoutSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId, fields = Some(emptySubscriptionFieldsWrapper))

    val subscriptionFieldValue = buildSubscriptionFieldValue(FieldName.random)
    val subscriptionFieldsWrapper = buildSubscriptionFieldsWrapper(applicationId, List(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersion.random, true, applicationId, fields = Some(subscriptionFieldsWrapper))

    val subscription1 = buildSubscription("Subscription1")
    val subscription2 = buildSubscription("Subscription2", versions = List(version1Subscribed, version2NotSubscribed))
    val subscription3 = buildSubscription("Subscription3", versions = List(version1Subscribed, version2NotSubscribed))
    val subscription4 = buildSubscription("Subscription4", versions = List(version1Subscribed, versionWithoutSubscriptionFields, versionWithSubscriptionFields))
  }

  trait AppWithSubscriptionDataSetup extends Setup with ApplicationBuilder {
    val applicationWithSubscriptionData = buildApplicationWithSubscriptionData()
  }

  trait AppWithSubscriptionDataAndFieldDefinitionsSetup extends AppWithSubscriptionDataSetup with FieldDefinitionsBuilder with ApiBuilder {
    val allFieldDefinitions = buildApiDefinitions()

    val apiContext = allFieldDefinitions.keySet.head
    val apiVersion = allFieldDefinitions(apiContext).keySet.head

    override val applicationWithSubscriptionData = buildApplicationWithSubscriptionData(apiContext, apiVersion)
  }

  "withApp" should {
    "fetch application" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      ApplicationServiceMock.FetchApplication.returns(application)

      val result = underTest.withApp(applicationId)(_ => {
        Future.successful(Ok(expectedResult))
      })

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult

      ApplicationServiceMock.FetchApplication.verifyParams(applicationId)
    }
  }

  "withAppAndSubsData" should {
    "fetch Application with Subscription Data" in new AppWithSubscriptionDataSetup {

      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)

      val result = underTest.withAppAndSubsData(applicationId)(_ => {
        Future.successful(Ok(expectedResult))
      })

      ApmServiceMock.verifyFetchApplicationById(applicationId)

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult
    }

  }

  "withAppAndSubscriptionsAndFieldDefinitions" should {
    "fetch Application with Subscription Data and Field Definitions" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
        val apiData = DefaultApiData.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
        val apiContextAndApiData = Map(apiContext -> apiData)

      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(apiContextAndApiData)

      val result = underTest.withAppAndSubscriptionsAndFieldDefinitions(applicationId)(applicationWithSubscriptionDataAndFieldDefinitions => {
        applicationWithSubscriptionDataAndFieldDefinitions.apiDefinitions.nonEmpty shouldBe true
        applicationWithSubscriptionDataAndFieldDefinitions.apiDefinitions(apiContext).keySet.contains(apiVersion) shouldBe true
        
        Future.successful(Ok(expectedResult))
      })

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult

      ApmServiceMock.verifyFetchApplicationById(applicationId)
      ApmServiceMock.verifyGetAllFieldDefinitionsReturns(model.Environment.SANDBOX)
    }
  }

  "withAppAndSubscriptionsAndStateHistory" should {
    "fetch Application with Subscription Data and State History" in new AppWithSubscriptionDataSetup {
      ApmServiceMock.FetchApplicationById.returns(applicationWithSubscriptionData)
      ApplicationServiceMock.FetchStateHistory.returns(buildStateHistory(applicationWithSubscriptionData.application.id, State.PRODUCTION))

      val result = underTest.withAppAndSubscriptionsAndStateHistory(applicationId)( _ =>
        Future.successful(Ok(expectedResult))
      )

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult

      ApmServiceMock.verifyFetchApplicationById(applicationId)
      ApplicationServiceMock.FetchStateHistory.verifyParams(applicationId)
    }
  }
}
