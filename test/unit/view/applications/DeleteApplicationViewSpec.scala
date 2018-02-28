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

package unit.view.applications

import java.util.UUID

import config.AppConfig
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages.Implicits._
import play.api.mvc.Flash
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.Token
import uk.gov.hmrc.play.test.UnitSpec

import scala.collection.JavaConversions._

class DeleteApplicationViewSpec extends UnitSpec with OneServerPerSuite {
  trait Setup {
    val fakeRequest = FakeRequest()
    implicit val request = fakeRequest.copyFakeRequest(tags = fakeRequest.tags ++ Map(
      Token.NameRequestTag -> "test",
      Token.RequestTag -> "test"
    ))

    val application =
      ApplicationResponse(
        UUID.randomUUID(),
        "application1",
        "PRODUCTION",
        None,
        Set(Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@email.com", CollaboratorRole.DEVELOPER)),
        DateTime.now(),
        Standard(),
        ApplicationState()
      )

    val applicationWithHistory = ApplicationWithHistory(application, Seq.empty)
  }

  "delete application view" should {

    "show application information, including superuser only actions, when logged in as superuser" in new Setup {

      val result = views.html.applications.delete_application.apply(applicationWithHistory, true)(request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "button", "Delete Application") shouldBe true
      elementExistsByText(document, "td", "PRODUCTION")
    }

    "show application information, excluding superuser only actions, when logged in as non superuser" in new Setup {
      val result = views.html.applications.delete_application.apply(applicationWithHistory, false)(request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "a", "Delete Application") shouldBe false
    }

    def elementExistsByText(doc: Document, elementType: String, elementText: String): Boolean = {
      doc.select(elementType).exists(node => node.text == elementText)
    }
  }
}
