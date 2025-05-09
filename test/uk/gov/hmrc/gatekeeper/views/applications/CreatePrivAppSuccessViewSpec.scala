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

import org.jsoup.Jsoup

import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.AccessType
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.CreateApplicationSuccessView

class CreatePrivAppSuccessViewSpec extends CommonViewSpec {

  trait Setup {
    val createApplicationSuccessView = app.injector.instanceOf[CreateApplicationSuccessView]
  }

  "CreatePrivAppSuccess page" when {

    val appId      = ApplicationId.random
    val appName    = ApplicationName("This is my app name")
    val env        = Environment.PRODUCTION
    val clientId   = ClientId.random
    val totpSecret = "DSKL595KJDHK540K09421"

    "a privileged application is created" should {
      "render" in new Setup {

        val accessType = Some(AccessType.PRIVILEGED)
        val totp       = Some(TotpSecrets(totpSecret))

        implicit val loggedInUser: LoggedInUser = LoggedInUser(Some(""))

        val page: () => HtmlFormat.Appendable =
          () => createApplicationSuccessView(appId, appName, env, accessType, totp, clientId)(loggedInUser, messagesProvider, fakeRequest)

        page().contentType should include("text/html")

        val document = Jsoup.parse(page().body)

        elementExistsByText(document, "h1", appName.value) shouldBe true
        elementExistsByText(document, "h2", "Application added") shouldBe true
        document.body().toString.contains("This is your only chance to copy and save this application's TOTP.") shouldBe true
        elementExistsByText(document, "div", s"Application ID ${appId.value}") shouldBe true
        elementExistsByText(document, "div", s"Application name $appName") shouldBe true
        elementExistsByText(document, "div", s"Environment ${env.displayText}") shouldBe true
        elementExistsByText(document, "div", "Access type Privileged") shouldBe true
        elementExistsByText(document, "div", s"TOTP secret $totpSecret") shouldBe true
        elementExistsByText(document, "div", s"Client ID ${clientId.value}") shouldBe true

      }
    }
  }
}
