/*
 * Copyright 2018 HM Revenue & Customs
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

package unit.view.developers

import config.AppConfig
import model.{Developer, UnverifiedStatus, VerifiedStatus}
import org.jsoup.Jsoup
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.i18n.Messages.Implicits.applicationMessages
import play.api.test.FakeRequest
import unit.utils.ViewHelpers._


class DeveloperDetailsViewSpec extends PlaySpec with OneServerPerSuite {
  "developer details view" must {
    implicit val request = FakeRequest()

    "show developer details when logged in as superuser" in {

      val developer: Developer = Developer("email@address.com", "firstname", "lastName", None, Seq())

      val developerEmail = developer.email
      val developerFirstName = developer.firstName

      val result = views.html.developers.developer_details.render(developer, request, None, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType must include("text/html")
      elementExistsByText(document, "h1", developerEmail) mustBe true
      document.getElementById("first-name").text mustBe developer.firstName
      document.getElementById("last-name").text mustBe developer.lastName
      document.getElementById("status").text mustBe (developer.status match {
        case UnverifiedStatus => "not yet verified"
        case VerifiedStatus => "verified"
        case _ => "unregistered"
      })
    }
  }
}
