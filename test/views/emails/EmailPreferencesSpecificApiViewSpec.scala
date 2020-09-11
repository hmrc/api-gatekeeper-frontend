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
import model.TopicOptionChoice.TopicOptionChoice
import model.{APIDefinition, LoggedInUser, TopicOptionChoice, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSpecificApiView

class EmailPreferencesSpecificApiViewSpec extends CommonViewSpec with UserTableHelper with EmailUsersHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView = app.injector.instanceOf[EmailPreferencesSpecificApiView]
  }

  def validateStaticPageElements(document: Document, filterButtonText: String, selectedTopic: Option[TopicOptionChoice]) {
    validatePageHeader(document, "Email users interested in a specific API")
    validateFormDestination(document, "api-filters", "/api-gatekeeper/emails/email-preferences/select-api")
    validateFormDestination(document, "topic-filter", "/api-gatekeeper/emails/email-preferences/by-specific-api")
    validateButtonText(document, "filter", filterButtonText)
    validateTopicGrid(document, selectedTopic)
  }

  "email preferences specific api view" must {
    val selectedTopic = TopicOptionChoice.BUSINESS_AND_POLICY
    val selectedApis: Seq[APIDefinition] = Seq(simpleAPIDefinition("Api1ServiceName", "Api1Name"),
      simpleAPIDefinition("Api2ServiceName", "Api2Name"))
    val user1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
    val user2 = User("user2@hmrc.com", "userB", "2", verified = Some(true))
    val users = Seq(user1, user2)

    "show correct title and elements on initial load" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, "", Seq.empty, None, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)
      validateStaticPageElements(document, "Filter", None)
      verifyTableHeader(document, tableIsVisible = false)
    }

    "show correct title and elements when topic filter provided but nothing else" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, "", Seq.empty, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)

      validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
      verifyTableHeader(document, tableIsVisible = false)
    }

    "show correct title and elements when topic filter provided and selectedApis" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, "", selectedApis, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)

      validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
      verifyTableHeader(document, tableIsVisible = false)
      validateSelectedSpecificApiItems(document, selectedApis)
    }

     "show correct title and elements when topic filter provided, selectedApis and list of users and emails" in new Setup {
       val emailsStr = users.map(_.email).mkString(";")
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(users, emailsStr, selectedApis, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)
      
      validateStaticPageElements(document, "Filter Again", Some(selectedTopic))
      validateSelectedSpecificApiItems(document, selectedApis)

      verifyTableHeader(document)
      verifyUserRow(document, user1)
      verifyUserRow(document, user2)
      validateCopyToClipboardValue(document, emailsStr)
    }
  }

}
