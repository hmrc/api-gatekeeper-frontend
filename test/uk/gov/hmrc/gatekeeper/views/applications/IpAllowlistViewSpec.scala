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

package uk.gov.hmrc.gatekeeper.views.applications

import java.time.{LocalDateTime, Period}

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat.Appendable

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, IpAllowlist}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.IpAllowlistView

class IpAllowlistViewSpec extends CommonViewSpec {

  trait Setup {
    val request                          = FakeRequest()
    val ipAllowlistView: IpAllowlistView = app.injector.instanceOf[IpAllowlistView]
    val grantLength: Period              = Period.ofDays(547)

    val application: ApplicationResponse =
      ApplicationResponse(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        "application1",
        "PRODUCTION",
        None,
        Set(
          Collaborators.Administrator(UserId.random, LaxEmailAddress("sample@example.com")),
          Collaborators.Developer(UserId.random, LaxEmailAddress("someone@example.com"))
        ),
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        Standard(),
        ApplicationState(),
        grantLength,
        ipAllowlist = IpAllowlist(allowlist = Set("1.1.1.1/24"))
      )
  }

  "IP allowlist view" should {
    "show IP allowlist information when the allowlist is not required" in new Setup {
      val result: Appendable = ipAllowlistView(application)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", "View IP allow list") shouldBe true
      elementExistsByText(document, "p", "The IP allow list is mandatory for this application.") shouldBe false
    }

    "show IP allowlist information when the allowlist is required" in new Setup {
      val result: Appendable = ipAllowlistView(application.copy(ipAllowlist = IpAllowlist(required = true, Set("1.1.1.1/24"))))(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", "View IP allow list") shouldBe true
      elementExistsByText(document, "p", "The IP allow list is mandatory for this application.") shouldBe true
    }
  }
}
