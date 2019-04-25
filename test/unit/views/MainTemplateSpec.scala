/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.views

import config.AppConfig
import org.mockito.BDDMockito.given
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.play.test.UnitSpec
import views.html
import play.api.i18n.Messages.Implicits.applicationMessages
import utils.LoggedInUser


class MainTemplateSpec extends UnitSpec with Matchers with MockitoSugar with OneServerPerSuite {

  "MainTemplate" should {

    implicit val mockConfig: AppConfig = mock[AppConfig]

    "Use the sandbox class when the environment is set to the Enhanced Sandbox" in {

      given(mockConfig.isExternalTestEnvironment).willReturn(true)

      implicit val loggedInUser = LoggedInUser(Some("TestUser"))

      val mainView: Html = html.main("Test")(mainContent=HtmlFormat.empty)

      mainView.body should include("class=\"sandbox")
    }

    "Not use the sandbox class when the Enhanced Sandbox configuration is switched off" in {
      given(mockConfig.isExternalTestEnvironment).willReturn(false)

      implicit val loggedInUser = LoggedInUser(Some("TestUser"))

      val mainView: Html = html.main("Test")(mainContent=Html.apply("<h1>Dummy Header</h1>"))

      mainView.body should not include "class=\"sandbox"
    }
  }
}
