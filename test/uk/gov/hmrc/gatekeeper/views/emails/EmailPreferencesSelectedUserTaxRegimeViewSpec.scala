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
import uk.gov.hmrc.gatekeeper.views.html.emails.{EmailPreferencesSelectedTaxRegimeView, EmailPreferencesSelectedUserTaxRegimeView}

class EmailPreferencesSelectedUserTaxRegimeViewSpec extends CommonViewSpec with EmailPreferencesAPICategoryViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type]                                = FakeRequest().withCSRFToken
    val emailPreferencesSelectedUserTaxRegimeView: EmailPreferencesSelectedUserTaxRegimeView = app.injector.instanceOf[EmailPreferencesSelectedUserTaxRegimeView]
    val user1                                                                                = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
    val user2                                                                                = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
    val users                                                                                = Seq(user1, user2)
  }

  "email preferences selected category view" should {

    val category1          = ApiCategory.VAT
    val category2          = ApiCategory.AGENTS
    val category3          = ApiCategory.RELIEF_AT_SOURCE
    val categories         = Set[ApiCategory](category1, category2, category3)
    val category4          = ApiCategory.CUSTOMS
    val category5          = ApiCategory.EXAMPLE
    val categoriesSelected = Set[ApiCategory](category4, category5)
    val allCategories      = categoriesSelected ++ categories

    "show correct title and options with page1 results" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectedUserTaxRegimeView.render(users, "", categoriesSelected, 0, 2, 6, request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesSelectedTaxRegimePage(1, Jsoup.parse(result.body), users)
    }

    "show correct title and options with page2 results" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectedUserTaxRegimeView.render(users, "", categoriesSelected, 2, 2, 6, request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesSelectedTaxRegimePage(2, Jsoup.parse(result.body), users)
    }

    "show correct title and options with last page results" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectedUserTaxRegimeView.render(users, "", categoriesSelected, 4, 2, 6, request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesSelectedTaxRegimePage(3, Jsoup.parse(result.body), users)
    }
  }
}
