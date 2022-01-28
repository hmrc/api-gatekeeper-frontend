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

package views.emails

import mocks.config.AppConfigMock
import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser
import model._
import org.jsoup.Jsoup
import play.api.libs.json.JsArray
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailApiSubscriptionsView

class EmailAPISubscriptionsViewSpec extends CommonViewSpec with EmailAPISubscriptionsViewHelper with APIDefinitionHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken
    val emailRecipientsAsJson: JsArray = new JsArray()
    val emailApiSubscriptionsView: EmailApiSubscriptionsView = app.injector.instanceOf[EmailApiSubscriptionsView]
  }

  "email api subscriptions view" must {

    val user1 = RegisteredUser("user1@hmrc.com", UserId.random, "userA", "1", verified = true)
    val user2 = RegisteredUser("user2@hmrc.com", UserId.random, "userB", "2", verified = true)
    val users = Seq(user1, user2)
    val api1 = simpleAPIDefinition("api", "Magical API", "magical", None, "1")
    val api2 = simpleAPIDefinition("api", "Magical API", "magical", None, "2")
    val dropdownview1 = DropDownValue("magical__2", "Magical API (ALPHA)")
    val dropdownview2 = DropDownValue("magical__1", "Magical API (BETA)")
    val queryParams = Map("apiVersionFilter" -> dropdownview1.value)
    val dropdowns = Seq(dropdownview1, dropdownview2)

    "show correct title and select correct option when filter and users lists present" in new Setup {
      val result: HtmlFormat.Appendable =
        emailApiSubscriptionsView.render(dropdowns, users, emailRecipientsAsJson, s"${user1.email}; ${user2.email}", queryParams, request, LoggedInUser(None), messagesProvider)

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2), dropdownview1.value, users)
    }

    "show correct title and select correct option when filter present but no Users returned" in new Setup {
      val queryParams = Map("apiVersionFilter" -> dropdownview2.value)
      val result: HtmlFormat.Appendable = emailApiSubscriptionsView.render(dropdowns, Seq.empty, emailRecipientsAsJson, "", queryParams, request, LoggedInUser(None), messagesProvider)

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2), dropdownview2.value, Seq.empty)
    }

    "show correct title and select no options when no filter present and no Users returned" in new Setup {
      val queryParams: Map[String, String] = Map.empty
      val result: HtmlFormat.Appendable = emailApiSubscriptionsView.render(dropdowns, Seq.empty, emailRecipientsAsJson, "", queryParams, request, LoggedInUser(None), messagesProvider)

      validateEmailAPISubscriptionsPage(Jsoup.parse(result.body), Seq(api1, api2))
    }
  }

}
