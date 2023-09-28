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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailApiSubscriptionsView

class EmailAPISubscriptionsViewSpec extends CommonViewSpec with EmailAPISubscriptionsViewHelper with APIDefinitionHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken
    val emailApiSubscriptionsView: EmailApiSubscriptionsView  = app.injector.instanceOf[EmailApiSubscriptionsView]
  }

  "email api subscriptions view" must {

    val user1         = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
    val user2         = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
    val users         = Seq(user1, user2)
    val api1          = simpleAPIDefinition("api", "Magical API", "magical", Set(ApiCategory.OTHER), "1")
    val api2          = simpleAPIDefinition("api", "Magical API", "magical", Set(ApiCategory.OTHER), "2")
    val dropdownview1 = DropDownValue("magical__2", "Magical API (ALPHA)")
    val dropdownview2 = DropDownValue("magical__1", "Magical API (BETA)")
    val queryParams   = Map("apiVersionFilter" -> dropdownview1.value)
    val dropdowns     = Seq(dropdownview1, dropdownview2)

    "show correct title and select correct option when filter and users lists present" in new Setup {
      val result: HtmlFormat.Appendable =
        emailApiSubscriptionsView.render(
          dropdowns,
          users,
          s"${user1.email.text}; ${user2.email.text}",
          queryParams,
          request,
          LoggedInUser(None),
          messagesProvider
        )

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2), dropdownview1.value, users)
    }

    "show correct title and select correct option when filter present but no Users returned" in new Setup {
      val queryParams                   = Map("apiVersionFilter" -> dropdownview2.value)
      val result: HtmlFormat.Appendable =
        emailApiSubscriptionsView.render(dropdowns, Seq.empty, "", queryParams, request, LoggedInUser(None), messagesProvider)

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2), dropdownview2.value, Seq.empty)
    }

    "show correct title and select no options when no filter present and no Users returned" in new Setup {
      val queryParams: Map[String, String] = Map.empty
      val result: HtmlFormat.Appendable    =
        emailApiSubscriptionsView.render(dropdowns, Seq.empty, "", queryParams, request, LoggedInUser(None), messagesProvider)

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2))
    }
  }

}
