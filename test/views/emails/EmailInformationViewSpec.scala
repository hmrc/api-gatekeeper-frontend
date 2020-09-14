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
import model.{EmailOptionChoice, LoggedInUser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.emails.EmailInformationViewHelper
import views.html.emails.EmailInformationView

class EmailInformationViewSpec extends CommonViewSpec with EmailInformationViewHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailInformationPageView: EmailInformationView = app.injector.instanceOf[EmailInformationView]

  }

  "email information view" must {
    "show correct title and content for EMAIL_ALL_USERS" in new Setup {
      val result: HtmlFormat.Appendable = emailInformationPageView.render(EmailOptionChoice.EMAIL_ALL_USERS, request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      validateAllUsersInformationPage(document)
    }

    "show correct title and content for API_SUBSCRIPTION" in new Setup {
      val result: HtmlFormat.Appendable = emailInformationPageView.render(EmailOptionChoice.API_SUBSCRIPTION, request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)
      validateApiSubcriptionInformationPage(document)

    }
  }


}
