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
import scala.concurrent.Future

import play.api.http.Status.FORBIDDEN
import play.api.test.Helpers._

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class SubscriptionFieldsControllerSpec extends ControllerBaseSpec {

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView     = app.injector.instanceOf[ForbiddenView]

  trait Setup extends ControllerSetupBase {
    val apmService = mock[ApmService]

    val controller =
      new SubscriptionFieldsController(forbiddenView, mcc, errorTemplateView, StrideAuthorisationServiceMock.aMock, LdapAuthorisationServiceMock.aMock, apmService)
  }

  "subscriptionFieldValues" should {
    "return a csv" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val expectedValues = """|Environment,ClientId,ApiContext,ApiVersionNbr,FieldName
                              |PRODUCTION,my-client-id,my-api-context,my-api-version,callbackUrl
                              |""".stripMargin

      when(apmService.subsFieldsCsv()(*)).thenReturn(Future.successful(expectedValues))

      val result = controller.subscriptionFieldValues()(aLoggedInRequest)

      val expectedCsv = """|Environment,ClientId,ApiContext,ApiVersionNbr,FieldName
                           |PRODUCTION,my-client-id,my-api-context,my-api-version,callbackUrl
                           |""".stripMargin

      contentAsString(result) shouldBe expectedCsv
    }

    "Forbidden if not authenticated" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
      LdapAuthorisationServiceMock.Auth.notAuthorised

      val result = controller.subscriptionFieldValues()(aLoggedOutRequest)

      status(result) shouldBe FORBIDDEN
    }
  }
}
