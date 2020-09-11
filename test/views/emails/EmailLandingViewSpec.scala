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
import model.EmailOptionChoice._
import model.LoggedInUser
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.SendEmailChoiceView

class EmailLandingViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailLandingView: SendEmailChoiceView = app.injector.instanceOf[SendEmailChoiceView]
  }

  "email landing view" must {
    "show correct title and options" in new Setup {
      val result: Html = emailLandingView.render(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)
      elementExistsByText(document, "h1", "Send emails to users based on") mustBe true

      verifyEmailOptions(EMAIL_PREFERENCES, document, isDisabled = false)
      verifyEmailOptions(API_SUBSCRIPTION, document, isDisabled = false)
      verifyEmailOptions(EMAIL_ALL_USERS, document, isDisabled = false)
      elementExistsByIdWithAttr(document, EMAIL_PREFERENCES.toString, "checked") mustBe true
    }
  }

  def verifyEmailOptions(option: EmailOptionChoice, document: Document, isDisabled: Boolean): Unit ={
    elementExistsById(document, option.toString) mustBe true
    elementExistsContainsText(document, "label",  optionLabel(option)) mustBe true
    elementExistsContainsText(document, "label",  optionHint(option)) mustBe true
    elementExistsByIdWithAttr(document, option.toString, "disabled") mustBe isDisabled
  }
}
