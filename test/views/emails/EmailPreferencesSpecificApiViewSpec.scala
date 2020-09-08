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
import model.{LoggedInUser, TopicOptionChoice}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSpecificApiView

class EmailPreferencesSpecificApiViewSpec extends CommonViewSpec with UserTableHelper with EmailUsersHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView = app.injector.instanceOf[EmailPreferencesSpecificApiView]
  }

  "email preferences specific api view" must {

    "show correct title and options when no filter provided and empty list of users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")


      validatePageHeader(document, "Email users interested in a specific API")
      validateCopyToClipboardLink(document, isVisible = false)



    }
  }

}
