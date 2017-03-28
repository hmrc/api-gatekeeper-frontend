/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.controllers

import controllers.NavigationController
import org.mockito.BDDMockito._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers._
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

class NavigationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val materializer = fakeApplication.materializer

  running(fakeApplication) {

    trait Setup extends ControllerSetupBase {

      val underTest = new NavigationController {
        val appConfig = mockConfig
      }
    }

    "navigationController" should {

      "render all nav links for standard site" in new Setup {
        given(mockConfig.isExternalTestEnvironment).willReturn(false)
        val result = await(underTest.navLinks()(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) shouldBe """[{"label":"Dashboard","href":"/api-gatekeeper/dashboard"},{"label":"Applications","href":"/api-gatekeeper/applications"},{"label":"Developers","href":"/api-gatekeeper/developers"},{"label":"API Approvals","href":"/api-service-approval/pending"}]"""
      }


      "render all nav links for ET site" in new Setup {
        given(mockConfig.isExternalTestEnvironment).willReturn(true)
        val result = await(underTest.navLinks()(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) shouldBe """[{"label":"Applications","href":"/api-gatekeeper/applications"},{"label":"Developers","href":"/api-gatekeeper/developers"},{"label":"API Approvals","href":"/api-service-approval/pending"}]"""
      }
    }
  }
}
