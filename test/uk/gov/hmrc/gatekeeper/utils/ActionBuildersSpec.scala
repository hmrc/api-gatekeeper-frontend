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

package uk.gov.hmrc.gatekeeper.utils

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.pekko.stream.Materializer

import play.api.i18n.Messages
import play.api.mvc.Results.Ok
import play.api.mvc.{MessagesRequest, _}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.State
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{LoggedInRequest, _}
import uk.gov.hmrc.apiplatform.modules.subscriptionfields.domain.models.FieldName
import uk.gov.hmrc.gatekeeper.builder.{ApiBuilder, ApplicationBuilder, FieldDefinitionsBuilder, SubscriptionsBuilder}
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.controllers.{ControllerBaseSpec, ControllerSetupBase}
import uk.gov.hmrc.gatekeeper.models.ApplicationWithSubscriptionFieldsAndStateHistory
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationQueryService, ApplicationService}

class ActionBuildersSpec extends ControllerBaseSpec {

  trait Setup extends ControllerSetupBase {
    implicit val materializer: Materializer = app.materializer
    implicit val hc: HeaderCarrier          = HeaderCarrier()

    val underTest = new ActionBuilders {
      val applicationService: ApplicationService           = mockApplicationService
      val applicationQueryService: ApplicationQueryService = ApplicationQueryServiceMock.aMock
      val apmService: ApmService                           = mockApmService
      val errorHandler: ErrorHandler                       = app.injector.instanceOf[ErrorHandler]
    }

    val fakeRequest                                                            = FakeRequest()
    val msgRequest                                                             = new MessagesRequest(fakeRequest, stubMessagesApi())
    implicit val aUserLoggedInRequest: LoggedInRequest[AnyContentAsEmpty.type] = new LoggedInRequest[AnyContentAsEmpty.type](Some("username"), GatekeeperRoles.USER, msgRequest)
    implicit val messages: Messages                                            = mcc.messagesApi.preferred(aUserLoggedInRequest)

    val actionReturns200Body: Request[_] => HeaderCarrier => Future[Result] = _ => _ => Future.successful(Results.Ok)

    val expectedResult = "result text"
  }

  trait WithSubscription extends Setup with SubscriptionsBuilder {

    val subscription = buildSubscription(
      "mySubscription",
      versions = List(
        buildVersionWithSubscriptionFields(ApiVersionNbr.random, true, applicationId),
        buildVersionWithSubscriptionFields(ApiVersionNbr.random, true, applicationId)
      )
    )
  }

  trait SubscriptionsWithMixOfSubscribedVersionsSetup extends WithSubscription {
    val version1Subscribed    = buildVersionWithSubscriptionFields(ApiVersionNbr.random, true, applicationId)
    val version2NotSubscribed = buildVersionWithSubscriptionFields(ApiVersionNbr.random, false, applicationId)

    val emptySubscriptionFieldsWrapper   = buildSubscriptionFieldsWrapper(applicationId)
    val versionWithoutSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersionNbr.random, true, applicationId, fields = Some(emptySubscriptionFieldsWrapper))

    val subscriptionFieldValue        = buildSubscriptionFieldValue(FieldName.random)
    val subscriptionFieldsWrapper     = buildSubscriptionFieldsWrapper(applicationId, List(subscriptionFieldValue))
    val versionWithSubscriptionFields = buildVersionWithSubscriptionFields(ApiVersionNbr.random, true, applicationId, fields = Some(subscriptionFieldsWrapper))

    val subscription1 = buildSubscription("Subscription1")
    val subscription2 = buildSubscription("Subscription2", versions = List(version1Subscribed, version2NotSubscribed))
    val subscription3 = buildSubscription("Subscription3", versions = List(version1Subscribed, version2NotSubscribed))
    val subscription4 = buildSubscription("Subscription4", versions = List(version1Subscribed, versionWithoutSubscriptionFields, versionWithSubscriptionFields))
  }

  trait AppWithSubscriptionDataSetup extends Setup with ApplicationBuilder {
    val applicationWithSubsFields = buildAppWithSubsFields()
  }

  trait AppWithSubscriptionDataAndFieldDefinitionsSetup extends AppWithSubscriptionDataSetup with FieldDefinitionsBuilder with ApiBuilder {
    val allFieldDefinitions = buildApiDefinitionFields()

    val apiContext = allFieldDefinitions.keySet.head
    val apiVersion = allFieldDefinitions(apiContext).keySet.head

    override val applicationWithSubsFields = buildAppWithSubsFields(apiContext, apiVersion)
  }

  "withApp" should {
    "fetch application" in new SubscriptionsWithMixOfSubscribedVersionsSetup {
      ApplicationQueryServiceMock.FetchApplication.returns(application)

      val result = underTest.withApp(applicationId)(_ => {
        Future.successful(Ok(expectedResult))
      })

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult
    }
  }

  "withAppAndSubsData" should {
    "fetch Application with Subscription Fields" in new AppWithSubscriptionDataSetup {

      ApplicationQueryServiceMock.FetchAppWithSubsFields.returns(applicationWithSubsFields)

      val result = underTest.withAppWithSubsFields(applicationWithSubsFields.id)(_ => {
        Future.successful(Ok(expectedResult))
      })

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult
    }
  }

  "withAppAndSubscriptionsAndFieldDefinitions" should {
    "fetch Application with Subscription Fields and Field Definitions" in new AppWithSubscriptionDataAndFieldDefinitionsSetup {
      val apiDefinition = DefaultApiDefinition.withName("API NAme").addVersion(VersionOne, DefaultVersionData)
      val possibleSubs  = List(apiDefinition)

      ApmServiceMock.FetchApplicationById.returns(applicationWithSubsFields)
      ApmServiceMock.getAllFieldDefinitionsReturns(allFieldDefinitions)
      ApmServiceMock.fetchAllPossibleSubscriptionsReturns(possibleSubs)

      val result = underTest.withAppAndSubscriptionsAndFieldDefinitions(applicationId)(applicationWithSubscriptionDataAndFieldDefinitions => {
        applicationWithSubscriptionDataAndFieldDefinitions.apiDefinitionFields.nonEmpty shouldBe true
        applicationWithSubscriptionDataAndFieldDefinitions.apiDefinitionFields(apiContext).keySet.contains(apiVersion) shouldBe true

        Future.successful(Ok(expectedResult))
      })

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult

      ApmServiceMock.verifyFetchApplicationById(applicationId)
      ApmServiceMock.verifyGetAllFieldDefinitionsReturns(Environment.SANDBOX)
    }
  }

  "withAppAndSubscriptionsAndStateHistory" should {
    "fetch Application with Subscription Fields and State History" in new AppWithSubscriptionDataSetup {
      val stateHistory = List(buildStateHistory(applicationWithSubsFields.id, State.PRODUCTION))
      val app          = ApplicationWithSubscriptionFieldsAndStateHistory(applicationWithSubsFields, stateHistory)
      ApplicationQueryServiceMock.FetchAppWithSubsFieldsAndHistory.returns(app)

      val result = underTest.withAppWithSubsFieldsAndHistory(app.id)(_ =>
        Future.successful(Ok(expectedResult))
      )

      status(result) shouldBe OK
      contentAsString(result) shouldBe expectedResult
    }
  }
}
