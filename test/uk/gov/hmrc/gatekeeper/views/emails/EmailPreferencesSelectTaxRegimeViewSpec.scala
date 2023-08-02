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

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesSelectTaxRegimeView

class EmailPreferencesSelectTaxRegimeViewSpec extends CommonViewSpec with EmailPreferencesAPICategoryViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSelectTaxRegimeView: EmailPreferencesSelectTaxRegimeView = app.injector.instanceOf[EmailPreferencesSelectTaxRegimeView]
  }

  "email preferences category view" should {

    val category1          = APICategoryDetails("VAT", "Vat")
    val category2          = APICategoryDetails("AGENT", "Agents")
    val category3          = APICategoryDetails("RELIEF_AT_SOURCE", "Relief at source")
    val categories         = List(category1, category2, category3)
    val category4          = APICategoryDetails("CUSTOMS", "Customs")
    val category5          = APICategoryDetails("EXAMPLE", "Example")
    val categoriesSelected = List(category4, category5)
    val allCategories      = categoriesSelected ++ categories

    "show correct title and options when no filter provided " in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectTaxRegimeView.render(allCategories, List(), request, LoggedInUser("Bobby Example"), messagesProvider)

      validateEmailPreferencesSelectTaxRegimeResultsPage(Jsoup.parse(result.body), allCategories, "/api-gatekeeper/emails/email-preferences/by-specific-tax-regime")
    }

    "show correct title and options when selectedAPis are provided" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectTaxRegimeView.render(allCategories, categoriesSelected, request, LoggedInUser("Bobby Example"), messagesProvider)
      validateSelectTaxRegimePageWithPreviouslySelectedTaxRegimes(
        Jsoup.parse(result.body),
        allCategories,
        categoriesSelected,
        "/api-gatekeeper/emails/email-preferences/by-specific-tax-regime"
      )
    }
  }
}
