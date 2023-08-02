/*
 * Copyright 2023 HM Revenue & Customs
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
import org.jsoup.Jsoup

import play.api.libs.json.JsArray
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models.APIAccessType.PUBLIC
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesSpecificApiView

class EmailPreferencesSpecificApiViewSpec extends CommonViewSpec with EmailPreferencesSpecificAPIViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSpecificApiView: EmailPreferencesSpecificApiView = app.injector.instanceOf[EmailPreferencesSpecificApiView]
  }

  "email preferences specific api view" must {
    val selectedTopic    = TopicOptionChoice.BUSINESS_AND_POLICY
    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", List(CombinedApiCategory("CUSTOMS")), ApiType.REST_API, Some(PUBLIC))
    val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", List(CombinedApiCategory("VAT")), ApiType.XML_API, Some(PUBLIC))
    val combinedList     = List(combinedRestApi1, combinedXmlApi2)

    val user1 = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
    val user2 = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
    val users = List(user1, user2)

    "show correct title and elements on initial load" in new Setup {
      val result: HtmlFormat.Appendable = {
        emailPreferencesSpecificApiView.render(List.empty, new JsArray(), "", List.empty, None, request, LoggedInUser("Bobby Example"), messagesProvider)
      }
      validateEmailPreferencesSpecificApiPage(Jsoup.parse(result.body), List.empty)
    }

    "show correct title and elements when topic filter provided but nothing else" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(List.empty, new JsArray(), "", List.empty, Some(selectedTopic), request, LoggedInUser("Bobby Example"), messagesProvider)
      validateEmailPreferencesSpecificAPIWithOnlyTopicFilter(Jsoup.parse(result.body), selectedTopic)
    }

    "show correct title and elements when topic filter provided and selectedApis" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(List.empty, new JsArray(), "", combinedList, Some(selectedTopic), request, LoggedInUser("Bobby Example"), messagesProvider)

      validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), selectedTopic, combinedList, List.empty, "")
    }

    "show correct title and elements when topic filter provided, selectedApis and list of users and emails" in new Setup {
      val emails                        = users.map(_.email.text).sorted.mkString("; ")
      val result: HtmlFormat.Appendable =
        emailPreferencesSpecificApiView.render(users, new JsArray(), emails, combinedList, Some(selectedTopic), request, LoggedInUser("Bobby Example"), messagesProvider)

      validateEmailPreferencesSpecificAPIResults(Jsoup.parse(result.body), selectedTopic, combinedList)
    }
  }
}
