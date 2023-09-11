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

import akka.stream.Materializer

import play.api.http.Status.FORBIDDEN
import play.api.mvc.{AnyContent, Request}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationServiceMockModule
import uk.gov.hmrc.gatekeeper.models.Environment.PRODUCTION
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class ApiDefinitionControllerSpec extends ControllerBaseSpec {

  implicit lazy val request: Request[AnyContent] = FakeRequest()
  implicit lazy val materializer: Materializer   = app.materializer
  implicit val hc                                = HeaderCarrier()

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView     = app.injector.instanceOf[ForbiddenView]

  trait Setup extends ControllerSetupBase with StrideAuthorisationServiceMockModule {

    val controller = new ApiDefinitionController(
      mockApiDefinitionService,
      forbiddenView,
      mcc,
      errorTemplateView,
      StrideAuthorisationServiceMock.aMock,
      LdapAuthorisationServiceMock.aMock
    )
  }

  "apis" should {
    "return a csv" in new Setup {
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val someContext = ApiContext.random

      val apiVersions   = List(
        ApiVersionGK(ApiVersionNbr("1.0"), ApiVersionSource.UNKNOWN, ApiStatus.ALPHA),
        ApiVersionGK(ApiVersionNbr("2.0"), ApiVersionSource.OAS, ApiStatus.STABLE)
      )
      val apiDefinition = ApiDefinitionGK("aServiceName", "", name = "MyApi", "", someContext, apiVersions, None, None)

      Apis.returns((apiDefinition, PRODUCTION))

      val result = controller.apis()(aLoggedInRequest)

      contentAsString(result) shouldBe s"""name,serviceName,context,version,source,status,access,isTrial,environment
                                          |MyApi,aServiceName,${someContext.value},1.0,UNKNOWN,Alpha,PUBLIC,false,PRODUCTION
                                          |MyApi,aServiceName,${someContext.value},2.0,OAS,Stable,PUBLIC,false,PRODUCTION
                                          |""".stripMargin
    }

    "Forbidden if not stride auth" in new Setup {
      StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
      LdapAuthorisationServiceMock.Auth.notAuthorised

      val result = controller.apis()(aLoggedOutRequest)

      status(result) shouldBe FORBIDDEN
    }
  }
}
