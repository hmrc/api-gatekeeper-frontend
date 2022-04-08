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

package controllers

import model._
import play.api.http.Status._
import views.html.{ErrorTemplate, ForbiddenView}
import play.api.test.Helpers._
import play.api.http.Status.FORBIDDEN
import scala.concurrent.ExecutionContext.Implicits.global
import services.SubscriptionFieldsService
import java.util.UUID
import scala.concurrent.Future

class SubscriptionFieldsControllerSpec extends ControllerBaseSpec {

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]

  trait Setup extends ControllerSetupBase {
    val subscriptionFieldsService = mock[SubscriptionFieldsService]
    val controller = new SubscriptionFieldsController(subscriptionFieldsService,forbiddenView, mcc, errorTemplateView, strideAuthConfig, mockAuthConnector, forbiddenHandler)
  }
  
  "subscriptionFieldValues" should {
    "return a csv" in new Setup {
      givenTheGKUserIsAuthorisedAndIsANormalUser()
      
      val expectedValues = List(SubscriptionFields.ApplicationApiFieldValues(
        ClientId("my-client-id"),
        ApiContext("my-api-context"),
        ApiVersion("my-api-version"),
        UUID.randomUUID(),
        Map(FieldName("callbackUrl") -> FieldValue("callbackUrlValue"))
      ))

      when(subscriptionFieldsService.fetchAllFieldValues()(*))
        .thenReturn(Future.successful(expectedValues))
      
      val result = controller.subscriptionFieldValues()(aLoggedInRequest)

      val expectedCsv = """
        |Environment,ClientId,ApiContext,ApiVersion,FieldName
        |PRODUCTION,my-client-id,my-api-context,my-api-version,callbackUrl""".stripMargin.trim()

      contentAsString(result) shouldBe expectedCsv
    }

    "Forbidden if not stride auth" in new Setup {
      givenTheGKUserHasInsufficientEnrolments()
      
      val result = controller.subscriptionFieldValues()(aLoggedOutRequest)

      status(result) shouldBe FORBIDDEN
    }
  }
}
