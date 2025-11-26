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

package uk.gov.hmrc.gatekeeper.models.view

import org.scalatestplus.play.guice.GuiceOneAppPerTest

import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

class NavLinkSpec extends AsyncHmrcSpec with GuiceOneAppPerTest {

  "StaticNavLinks" should {
    "return static navigation items for gatekeeper" in {
      val apiUrl  = "https://admin.qa.tax.service.gov.uk/api-gatekeeper-apis"
      val touUrl  = "https://admin.qa.tax.service.gov.uk/api-gatekeeper-approvals/terms-of-use"
      val xmlUrl  = "https://admin.qa.tax.service.gov.uk/api-gatekeeper-xml-services/organisations"
      val request = FakeRequest(
        "GET",
        "https://admin.qa.tax.service.gov.uk/api-gatekeeper/applications?page=1&includeDeleted=true&search=pete&apiSubscription=&status=EXCLUDING_DELETED&accessType=&environment=PRODUCTION&showExport=&main-submit=Submit&pageSize=100"
      ).withSession()

      StaticNavLinks(apiUrl, touUrl, xmlUrl)(request) shouldBe
        Seq(
          NavLink("APIs", apiUrl, false, false, false, false),
          NavLink("Applications", "/api-gatekeeper/applications", true, false, false, false),
          NavLink("Developers", "/api-gatekeeper/developers", false, false, false, false),
          NavLink("Terms of use", touUrl, false, false, false, false),
          NavLink("Email", "/api-gatekeeper/emails", false, false, false, false),
          NavLink("API Approvals", "/api-gatekeeper/api-approvals", false, false, false, false),
          NavLink("XML", xmlUrl, false, false, false, false)
        )
    }
  }
}
