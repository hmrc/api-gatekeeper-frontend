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
import model.{DropDownValue, LoggedInUser, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.{EmailAllUsersView, EmailApiSubscriptionsView}

class EmailApiSubscriptionsViewSpec extends CommonViewSpec {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailApiSubscriptionsView: EmailApiSubscriptionsView = app.injector.instanceOf[EmailApiSubscriptionsView]

  }

  "email api subscriptions view" must {

    val user1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
    val user2 = User("user2@hmrc.com", "userB", "2", verified = Some(true))
    val users = Seq(user1, user2)
    val dropdownview1 = DropDownValue("magical_api__2","Magical API (ALPHA)")
    val dropdownview2 = DropDownValue("magical_api__1","Magical API (BETA)")
    val queryParams = Map("apiVersionFilter"-> dropdownview1.value)
    val apis = Seq(dropdownview1, dropdownview2)

    "show correct title and select correct option when filter and users lists present" in new Setup {
      val result: HtmlFormat.Appendable = emailApiSubscriptionsView.render(apis, users, s"${user1.email}; ${user2.email}",queryParams, request, LoggedInUser(None), messagesProvider)
      val tableIsVisible = true
      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", "Email all users subscribed to an API") mustBe true
      elementExistsContainsText(document, "div", s"${users.size} results") mustBe true
      elementExistsByAttr(document, "a", "data-clip-text") mustBe true
      elementExistsByText(document, "option", dropdownview1.description) mustBe true
      elementExistsByText(document, "option", dropdownview2.description) mustBe true
      getSelectedOptionValue(document) mustBe Some(dropdownview1.value)
      verifyTableHeader(document, tableIsVisible)
      verifyUserRow(document, user1)
      verifyUserRow(document, user2)
    }

    "show correct title and select correct option when filter present but no Users returned" in new Setup {
      val queryParams = Map("apiVersionFilter"-> dropdownview2.value)
      val result: HtmlFormat.Appendable = emailApiSubscriptionsView.render(apis, Seq.empty, "",queryParams, request, LoggedInUser(None), messagesProvider)
      val tableIsVisible = false
      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", "Email all users subscribed to an API") mustBe true
      elementExistsContainsText(document, "div", s"0 results") mustBe true
      elementExistsByAttr(document, "a", "data-clip-text") mustBe false
      getSelectedOptionValue(document) mustBe Some(dropdownview2.value)

      elementExistsByText(document, "option", dropdownview1.description) mustBe true
      elementExistsByText(document, "option", dropdownview2.description) mustBe true
      verifyTableHeader(document, tableIsVisible)
    }

    "show correct title and select no options when no filter present and no Users returned" in new Setup {
      val queryParams = Map("apiVersionFilter"-> "")
      val result: HtmlFormat.Appendable = emailApiSubscriptionsView.render(apis, Seq.empty, "",queryParams, request, LoggedInUser(None), messagesProvider)
      val tableIsVisible = false
      val document: Document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", "Email all users subscribed to an API") mustBe true
      elementExistsContainsText(document, "div", s"0 results") mustBe true
      elementExistsByAttr(document, "a", "data-clip-text") mustBe false
      getSelectedOptionValue(document) mustBe None

      elementExistsByText(document, "option", dropdownview1.description) mustBe true
      elementExistsByText(document, "option", dropdownview2.description) mustBe true
      verifyTableHeader(document, tableIsVisible)
    }

    def verifyUserRow(document: Document, user: User): Unit ={
      elementExistsByText(document, "td", user.email) mustBe true
      elementExistsByText(document, "td", user.firstName) mustBe true
      elementExistsByText(document, "td", user.lastName) mustBe true
    }

    def verifyTableHeader(document: Document, tableIsVisible: Boolean = false): Unit ={
      elementExistsByText(document, "th", "Email") mustBe tableIsVisible
      elementExistsByText(document, "th", "First name") mustBe tableIsVisible
      elementExistsByText(document, "th", "Last name") mustBe tableIsVisible
    }
  }

}
