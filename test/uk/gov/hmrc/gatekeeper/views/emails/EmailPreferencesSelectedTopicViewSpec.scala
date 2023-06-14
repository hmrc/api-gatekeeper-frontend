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
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice.BUSINESS_AND_POLICY
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesSelectedTopicView

class EmailPreferencesSelectedTopicViewSpec extends CommonViewSpec with EmailPreferencesAPICategoryViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type]                = FakeRequest().withCSRFToken
    val emailRecipientsAsJson: JsArray                                       = new JsArray()
    val emailPreferencesSelectedTopicView: EmailPreferencesSelectedTopicView = app.injector.instanceOf[EmailPreferencesSelectedTopicView]
    val user1                                                                = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
    val user2                                                                = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
    val users                                                                = Seq(user1, user2)
    val categories                                                           = List()
    val expectedTitle                                                        = "Email users interested in a specific API"
  }

  "email preferences selected api topic view" should {

    "show correct title and options when no filter provided and empty list of users" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectedTopicView.render(
          users,
          emailRecipientsAsJson,
          "",
          Some(BUSINESS_AND_POLICY),
          categories,
          "",
          "",
          List(),
          0,
          1,
          2,
          request,
          LoggedInUser(None),
          messagesProvider
        )

      validateEmailPreferencesSelectedApiTopicPage(Jsoup.parse(result.body), users)
    }
  }
}
