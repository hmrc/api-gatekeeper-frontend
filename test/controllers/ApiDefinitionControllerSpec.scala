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

import model._
import model.Environment.PRODUCTION
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class ApiDefinitionControllerSpec extends UnitSpec with WithFakeApplication with MockitoSugar {
  implicit val materializer = fakeApplication.materializer

  trait Setup extends ControllerSetupBase {
    val controller = new ApiDefinitionController(mockApiDefinitionService, mockAuthConnector)(mockConfig, implicitly)
  }
  
  "apis" should {
    "return a csv" in new Setup {
      givenTheUserIsAuthorisedAndIsANormalUser()

      val apiVersions = List(APIVersion("1.0", APIStatus.ALPHA), APIVersion("2.0", APIStatus.STABLE))
      val apiDefinition = APIDefinition("", "", name = "MyApi", "", "", apiVersions, None)
      
      given(mockApiDefinitionService.apis(any[HeaderCarrier]))
        .willReturn(Seq((apiDefinition, PRODUCTION) ))
      
      val result = await(controller.apis()(aLoggedInRequest))

      bodyOf(result) shouldBe """name,version,status,access,isTrial,environment
                                |MyApi,1.0,Alpha,PUBLIC,false,PRODUCTION
                                |MyApi,2.0,Stable,PUBLIC,false,PRODUCTION""".stripMargin
    }

    "Forbidden if not stride auth" in new Setup {
      givenTheUserHasInsufficientEnrolments()
      
      val result = await(controller.apis()(aLoggedOutRequest))

      status(result) shouldBe FORBIDDEN
    }
  }
}
