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
import model.{LoggedInUser, TopicOptionChoice, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesAPICategoryView
import model.APICategory

class EmailPreferencesAPICategoryViewSpec extends CommonViewSpec with UserTableHelper with EmailUsersHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesAPICategoryView: EmailPreferencesAPICategoryView = app.injector.instanceOf[EmailPreferencesAPICategoryView]
  }

  val expectedTitle = "Email users interested in a tax regime"

  def validateCategoryDropDown(document: Document, categories: List[APICategory])={
    for(category <- categories){
      withClue(s"Category: option `${category.category}` not in select list: ") {
        elementExistsByText(document, "option", category.name) mustBe true
      }
    }
  }

  def validateStaticPageElements(document: Document, categories: List[APICategory])={
      validatePageHeader(document, expectedTitle)
      validateCategoryDropDown(document, categories)
      checkElementsExistById(document, Seq( TopicOptionChoice.BUSINESS_AND_POLICY.toString,
        TopicOptionChoice.TECHNICAL.toString,  TopicOptionChoice.RELEASE_SCHEDULES.toString,  TopicOptionChoice.EVENT_INVITES.toString))
  }

  "email preferences category view" must {

    val user1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
    val user2 = User("user2@hmrc.com", "userB", "2", verified = Some(true))
    val users = Seq(user1, user2)

    val category1 = APICategory("VAT", "Vat")
    val category2 = APICategory("AGENT", "Agents")
    val category3 = APICategory("RELIEF_AT_SOURCE", "Relief at source")

    val categories = List(category1, category2, category3)

    "show correct title and options when no filter provided and empty list of users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesAPICategoryView.render(Seq.empty, "", None, categories, "", request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      validateStaticPageElements(document, categories)
      validateCopyToClipboardLink(document, isVisible = false)

      getSelectedOptionValue(document) mustBe None
      noInputChecked(document)

      verifyTableHeader(document, tableIsVisible = false)

    }


    "show correct title and options when only Topic filter provided" in new Setup {

      //If adding errors to the page we need to add tests in here for that message
      val result: HtmlFormat.Appendable =
        emailPreferencesAPICategoryView.render(Seq.empty, "", Some(TopicOptionChoice.BUSINESS_AND_POLICY), categories, "", request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      validateStaticPageElements(document, categories)
      validateCopyToClipboardLink(document, isVisible = false)

      getSelectedOptionValue(document) mustBe None
      isElementChecked(document, TopicOptionChoice.BUSINESS_AND_POLICY.toString)

      verifyTableHeader(document, tableIsVisible = false)

    }

    "show correct title and options when only Category filter provided" in new Setup {
        //If adding errors to the page we need to add tests in here for that message
      val result: HtmlFormat.Appendable =
        emailPreferencesAPICategoryView.render(Seq.empty, "", None, categories, category1.category, request, LoggedInUser(None), messagesProvider)
      val tableIsVisible = false
      val document: Document = Jsoup.parse(result.body)

      validateStaticPageElements(document, categories)
      validateCopyToClipboardLink(document, isVisible = false)

      getSelectedOptionValue(document) mustBe Some(category1.category)
      noInputChecked(document)

      verifyTableHeader(document, tableIsVisible)

    }

    "show correct title and select correct option when both filters and user lists are present" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesAPICategoryView.render(users, s"${user1.email}; ${user2.email}", Some(TopicOptionChoice.BUSINESS_AND_POLICY),  categories, category2.category, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)

       validateStaticPageElements(document, categories)
      elementExistsContainsText(document, "div", s"${users.size} results") mustBe true
      validateCopyToClipboardLink(document)
       getSelectedOptionValue(document) mustBe Some(category2.category)

      isElementChecked(document, TopicOptionChoice.BUSINESS_AND_POLICY.toString)

      verifyTableHeader(document)
      verifyUserRow(document, user1)
      verifyUserRow(document, user2)
    }

    "show correct title and select correct option when filter exists but no users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesAPICategoryView.render(Seq.empty, "", Some(TopicOptionChoice.RELEASE_SCHEDULES),  categories, category2.category, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)


      validateStaticPageElements(document, categories)
      validateCopyToClipboardLink(document, isVisible = false)
      getSelectedOptionValue(document) mustBe Some(category2.category)
      
      elementExistsContainsText(document, "div", "0 results") mustBe true
 
      isElementChecked(document, TopicOptionChoice.RELEASE_SCHEDULES.toString)

      verifyTableHeader(document, tableIsVisible = false)

    }

  }

}
