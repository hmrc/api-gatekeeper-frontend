/*
 * Copyright 2021 HM Revenue & Customs
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
import model._
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSpecificApiView

class EmailPreferencesSpecificApiViewSpec extends CommonViewSpec with EmailPreferencesSpecificAPIViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView = app.injector.instanceOf[EmailPreferencesSpecificApiView]
  }

  "email preferences specific api view" must {
    val selectedTopic = TopicOptionChoice.BUSINESS_AND_POLICY
    val selectedApis: Seq[ApiDefinition] = Seq(simpleAPIDefinition("Api1ServiceName", "Api1Name", "context", None, "1"),
      simpleAPIDefinition("Api2ServiceName", "Api2Name", "context",  None, "1"))
    val user1 = RegisteredUser("user1@hmrc.com", UserId.random, "userA", "1", verified = true)
    val user2 = RegisteredUser("user2@hmrc.com", UserId.random, "userB", "2", verified = true)
    val users = Seq(user1, user2)

    "show correct title and elements on initial load" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, "", Seq.empty, None, request, LoggedInUser(None), messagesProvider)

       validateEmailPreferencesSpecificAPIPage(Jsoup.parse(result.body), Seq.empty)
    }

    "show correct title and elements when topic filter provided but nothing else" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesSpecificApiView.render(Seq.empty, "", Seq.empty, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesSpecificAPIWithOnlyTopicFilter(Jsoup.parse(result.body),  selectedTopic)
    }

    "show correct title and elements when topic filter provided and selectedApis" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(Seq.empty, "", selectedApis, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), selectedTopic, selectedApis, Seq.empty, "")
    }

    "show correct title and elements when topic filter provided, selectedApis and list of users and emails" in new Setup {
      val emailsStr = users.map(_.email).sorted.mkString("; ")
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(users, emailsStr, selectedApis, Some(selectedTopic), request, LoggedInUser(None), messagesProvider)
     
      validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), selectedTopic, selectedApis, users, emailsStr)
    }
  }

}
