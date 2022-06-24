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

import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime

import services.ApmService
import model.{Environment,ApplicationId,ClientId}
import model.pushpullnotifications.{Box,BoxId,BoxCreator,BoxSubscriber,SubscriptionType}
import uk.gov.hmrc.modules.stride.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles

class BoxesControllerSpec extends ControllerBaseSpec {

  implicit val materializer = app.materializer

  running(app) {
    trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {
      val apmService: ApmService = mock[ApmService]

      val controller = new BoxesController(
        mcc,
        apmService,
        StrideAuthorisationServiceMock.aMock
      )
    }

    "BoxesController" should {
      "return a CSV of all boxes" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        val boxSubscriber = BoxSubscriber(
          "callbackUrl",
          DateTime.parse("2001-01-01T01:02:03"),
          SubscriptionType.API_PUSH_SUBSCRIBER
        )

        val box = Box(
          BoxId("boxId"),
          "boxName",
          BoxCreator(ClientId("clientId")), Some(ApplicationId("applicationId")),
          Some(boxSubscriber), Environment.PRODUCTION
        )

        when(apmService.fetchAllBoxes()((*))).thenReturn(Future.successful(List(box)))

        val expectedCsv = """|environment,applicationId,clientId,name,boxId,subscriptionType,callbackUrl
                             |PRODUCTION,applicationId,clientId,boxName,boxId,API_PUSH_SUBSCRIBER,callbackUrl
                             |""".stripMargin

        val result = controller.getAll()(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedCsv
      }

      "Forbidden if not stride auth" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments //succeeds(GatekeeperRoles.USER)
      
        val result = controller.getAll()(aLoggedOutRequest)

        status(result) shouldBe FORBIDDEN
      }
    }
  }
}
