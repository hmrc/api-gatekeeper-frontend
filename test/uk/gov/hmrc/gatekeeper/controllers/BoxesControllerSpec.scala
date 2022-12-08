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

package uk.gov.hmrc.gatekeeper.controllers

import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.joda.time.DateTime

import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.models.ClientId
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.{Box, BoxCreator, BoxId, BoxSubscriber, SubscriptionType}
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.models.Environment

class BoxesControllerSpec extends ControllerBaseSpec {

  implicit val materializer = app.materializer
  val anAppId               = ApplicationId.random
  val appIdText             = anAppId.value.toString()

  running(app) {
    trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {
      val apmService: ApmService = mock[ApmService]

      val controller = new BoxesController(
        mcc,
        apmService,
        StrideAuthorisationServiceMock.aMock,
        LdapAuthorisationServiceMock.aMock
      )
    }

    "BoxesController" should {
      val boxSubscriber = BoxSubscriber(
        "callbackUrl",
        DateTime.parse("2001-01-01T01:02:03"),
        SubscriptionType.API_PUSH_SUBSCRIBER
      )

      val box = Box(
        BoxId("boxId"),
        "boxName",
        BoxCreator(ClientId("clientId")),
        Some(anAppId),
        Some(boxSubscriber),
        Environment.PRODUCTION
      )

      val expectedCsv = s"""|environment,applicationId,clientId,name,boxId,subscriptionType,callbackUrl
                            |PRODUCTION,$appIdText,clientId,boxName,boxId,API_PUSH_SUBSCRIBER,callbackUrl
                            |""".stripMargin

      "return a CSV of all boxes" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

        when(apmService.fetchAllBoxes()((*))).thenReturn(Future.successful(List(box)))

        val result = controller.getAll()(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedCsv
      }

      "return a CSV of all boxes for LDAP auth" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.succeeds

        when(apmService.fetchAllBoxes()((*))).thenReturn(Future.successful(List(box)))

        val result = controller.getAll()(aLoggedInRequest)
        status(result) shouldBe OK
        contentAsString(result) shouldBe expectedCsv
      }

      "Forbidden if not authorised" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        LdapAuthorisationServiceMock.Auth.notAuthorised

        val result = controller.getAll()(aLoggedOutRequest)

        status(result) shouldBe FORBIDDEN
      }
    }
  }
}
