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
import views.html.emails.EmailPreferencesTopicView

class EmailPreferencesTopicViewSpec extends CommonViewSpec with EmailPreferencesTopicViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesTopicView: EmailPreferencesTopicView = app.injector.instanceOf[EmailPreferencesTopicView]
  }

  "email preferences topic view" must {

    val user1 = User("user1@hmrc.com", "userA", "1", verified = Some(true))
    val user2 = User("user2@hmrc.com", "userB", "2", verified = Some(true))
    val users = Seq(user1, user2)

    "show correct title and options when no filter provided and empty list of users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesTopicView.render(Seq.empty, "", None, request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesTopicPage(Jsoup.parse(result.body))
    }

    "show correct title and select correct option when filter and users lists present" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesTopicView.render(users, s"${user1.email}; ${user2.email}", Some(TopicOptionChoice.BUSINESS_AND_POLICY), request, LoggedInUser(None), messagesProvider)
      
      validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body), TopicOptionChoice.BUSINESS_AND_POLICY, users)
    }

    "show correct title and select correct option when filter exists but no users" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesTopicView.render(Seq.empty, "", Some(TopicOptionChoice.RELEASE_SCHEDULES), request, LoggedInUser(None), messagesProvider)
      
      validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body), TopicOptionChoice.RELEASE_SCHEDULES, Seq.empty)
    }

  }

}
