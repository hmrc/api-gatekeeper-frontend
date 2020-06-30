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

import mocks.config.AppConfigMock
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class NavigationControllerSpec extends ControllerBaseSpec {

  implicit val materializer = fakeApplication.materializer

  running(fakeApplication) {

    trait Setup extends ControllerSetupBase with AppConfigMock {

      val underTest = new NavigationController(mcc)
    }

    "navigationController" should {

      "render all nav links for standard site" in new Setup {
        val result = await(underTest.navLinks()(aLoggedInRequest))
        status(result) shouldBe OK
        bodyOf(result) shouldBe """[{"label":"Applications","href":"/api-gatekeeper/applications"},{"label":"Developers","href":"/api-gatekeeper/developers2"},{"label":"API Approvals","href":"/api-gatekeeper/pending"}]"""
      }
    }
  }
}
