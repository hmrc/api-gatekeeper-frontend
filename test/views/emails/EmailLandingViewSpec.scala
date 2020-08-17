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

package views.emails

import mocks.config.AppConfigMock
import model.LoggedInUser
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.SendEmailChoiceView

class EmailLandingViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request = FakeRequest().withCSRFToken

    val emailLandingView = app.injector.instanceOf[SendEmailChoiceView]

  }

  "email landing view" must {
    "show correct title" in new Setup {
      val result = emailLandingView.render(LoggedInUser(None), messagesProvider)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", "Send emails to users based on") mustBe true
    }
  }
}
