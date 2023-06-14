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
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.{EmailAllUsersNewView, EmailAllUsersView}

class EmailAllUsersNewViewSpec extends CommonViewSpec with EmailAllUsersViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken
    val emailRecipientsAsJson: JsArray                        = new JsArray()
    val emailAllUsersView: EmailAllUsersNewView               = app.injector.instanceOf[EmailAllUsersNewView]
  }

  "email all user view" must {

    "show correct title and content for 2 verified users" in new Setup {
      val user1                         = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
      val user2                         = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
      val users                         = Seq(user1, user2)
      val result: HtmlFormat.Appendable =
        emailAllUsersView.render(users, emailRecipientsAsJson, s"${user1.email.text}; ${user2.email.text}", 1, 2, users.size, request, LoggedInUser(None), messagesProvider)

      validateEmailAllUsersPaginatedPage(Jsoup.parse(result.body), 2, users)
    }

    "show correct title and content for empty / no users" in new Setup {
      val result: HtmlFormat.Appendable = emailAllUsersView.render(Seq.empty, emailRecipientsAsJson, "", 1, 2, 0, request, LoggedInUser(None), messagesProvider)

      validateEmailAllUsersPaginatedPage(Jsoup.parse(result.body), 0, Seq.empty)
    }

    "show correct paginated verified users" in new Setup {
      val user1                         = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
      val user2                         = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
      val user3                         = RegisteredUser("user3@hmrc.com".toLaxEmail, UserId.random, "userC", "3", verified = true)
      val users                         = Seq(user1, user2, user3)
      val result: HtmlFormat.Appendable =
        emailAllUsersView.render(
          users,
          emailRecipientsAsJson,
          s"${user1.email.text}; ${user2.email.text}; ${user3.email.text}",
          0,
          1,
          users.size,
          request,
          LoggedInUser(None),
          messagesProvider
        )

      validateEmailAllUsersPaginatedPage(Jsoup.parse(result.body), 3, users)
    }

  }

}