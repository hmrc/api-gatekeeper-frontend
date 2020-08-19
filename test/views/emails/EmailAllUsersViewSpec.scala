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
import model.{EmailOptionChoice, LoggedInUser, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.{EmailAllUsersView, EmailInformationView}

class EmailAllUsersViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailAllUsersView: EmailAllUsersView = app.injector.instanceOf[EmailAllUsersView]

  }

  "email all user view" must {
    "show correct title and content for 2 verified users" in new Setup {
      val users = Seq(User("user1@hmrc.com", "user", "1", Some(true)))
      val result: HtmlFormat.Appendable = emailAllUsersView.render(users, "user1.hmrc.com; user2@hmrc.com", request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", "Check you can email all users") mustBe true

    }
  }


}
