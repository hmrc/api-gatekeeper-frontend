/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.views.emails

import mocks.config.AppConfigMock
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import org.jsoup.Jsoup
import play.api.libs.json.JsArray
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesTopicView
class EmailPreferencesTopicViewSpec extends CommonViewSpec with EmailPreferencesTopicViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken
    val emailRecipientsAsJson: JsArray = new JsArray()
    val emailPreferencesTopicView: EmailPreferencesTopicView = app.injector.instanceOf[EmailPreferencesTopicView]
  }

  "email preferences topic view" must {

    val user1 = RegisteredUser("user1@hmrc.com", UserId.random, "userA", "1", verified = true)
    val user2 = RegisteredUser("user2@hmrc.com", UserId.random, "userB", "2", verified = true)
    val users = Seq(user1, user2)

    "show correct title and options when no filter provided and empty list of users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesTopicView.render(Seq.empty, emailRecipientsAsJson, "", None, request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesTopicPage(Jsoup.parse(result.body))
    }

    "show correct title and select correct option when filter and users lists present" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesTopicView.render(users, emailRecipientsAsJson, s"${user1.email}; ${user2.email}", Some(TopicOptionChoice.BUSINESS_AND_POLICY), request, LoggedInUser(None), messagesProvider)
      
      validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body), TopicOptionChoice.BUSINESS_AND_POLICY, users)
    }

    "show correct title and select correct option when filter exists but no users" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesTopicView.render(Seq.empty, emailRecipientsAsJson, "", Some(TopicOptionChoice.RELEASE_SCHEDULES), request, LoggedInUser(None), messagesProvider)
      
      validateEmailPreferencesTopicResultsPage(Jsoup.parse(result.body), TopicOptionChoice.RELEASE_SCHEDULES, Seq.empty)
    }

  }

}
