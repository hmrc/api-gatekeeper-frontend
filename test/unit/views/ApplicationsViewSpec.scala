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

/*
 * Copyright 2017 HM Revenue & Customs
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

package unit.views

import java.util.UUID

import config.AppConfig
import model.APIStatus._
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.OneServerPerSuite
import play.api.i18n.Messages.Implicits.applicationMessages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.play.test.UnitSpec
import views.html


class ApplicationsViewSpec extends UnitSpec with Matchers with MockitoSugar with OneServerPerSuite {

  "ApplicationsView" when {

    implicit val mockConfig: AppConfig = mock[AppConfig]
    implicit val userName = Option("Bob Dole")


    "Called with no APIs" should {
      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, Map.empty, false)

      "Display only subscription filters" in {
        val appView = applicationView()

        appView.body should include("<option value data-api-name>All applications</option>")
        appView.body should include("<option value=\"ANYSUB\" data-api-name=\"ANYSUB\">One or more subscriptions</option>")
        appView.body should include("<option value=\"NOSUB\" data-api-name=\"NOSUB\">No subscriptions</option>")
      }

      "Not include application state filters" in {
        val appView = applicationView()

        appView.body should not include "Stable"
        appView.body should not include "Beta"
        appView.body should not include "Retired"
        appView.body should not include "Deprecated"
      }

      "Display the Terms of Use filters" in {
        val appView = applicationView()

        appView.body should include("""<option id="default-tou-status" value data-api-name>All</option>""")
        appView.body should include("""<option value="TOU_NOT_ACCEPTED" data-api-name="TOU_NOT_ACCEPTED">Not agreed</option>""")
        appView.body should include("""<option value="TOU_ACCEPTED" data-api-name="TOU_ACCEPTED">Agreed</option>""")
      }
    }

    "Called with APIs" should {

      val apis = Map[String, Seq[VersionSummary]](
        displayedStatus(STABLE) -> Seq(VersionSummary("Dummy API", STABLE, APIIdentifier("dummy-api", "1.0"))),
        displayedStatus(BETA) -> Seq(VersionSummary("Beta API", BETA, APIIdentifier("beta-api", "1.0"))),
        displayedStatus(RETIRED) -> Seq(VersionSummary("Retired API", RETIRED, APIIdentifier("ret-api", "1.0"))),
        displayedStatus(DEPRECATED) -> Seq(VersionSummary("Deprecated API", DEPRECATED, APIIdentifier("dep-api", "1.0")))
      )

      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, apis, false)

      "Display the subscription filters" in {
        val appView = applicationView()

        appView.body should include("<option value data-api-name>All applications</option>")
        appView.body should include("<option value=\"ANYSUB\" data-api-name=\"ANYSUB\">One or more subscriptions</option>")
        appView.body should include("<option value=\"NOSUB\" data-api-name=\"NOSUB\">No subscriptions</option>")
      }

      "Include the application state filters" in {
        val appView = applicationView()

        appView.body should include ("Stable")
        appView.body should include ("Beta")
        appView.body should include ("Retired")
        appView.body should include ("Deprecated")
      }
    }

    "Called with application" should {

      val applications = Seq[DetailedSubscribedApplicationResponse](
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Testing App", Some("Testing App"), Set.empty, DateTime.now(), ApplicationState(State.TESTING), Standard(), Seq.empty, termsOfUseAgreed = true),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Pending Gatekeeper Approval App", Some("Pending Gatekeeper Approval App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_GATEKEEPER_APPROVAL), Ropc(), Seq.empty, termsOfUseAgreed = true),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Pending Requester Verification App", Some("Pending Requester Verification App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_REQUESTER_VERIFICATION), Privileged(), Seq.empty, termsOfUseAgreed = true),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Production App", Some("Production App"), Set.empty, DateTime.now(), ApplicationState(State.PRODUCTION), Standard(), Seq.empty, termsOfUseAgreed = true)
      )

      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(applications, Map.empty, false)

      "Display all four applications in all four states" in {
        val appView = applicationView()

        appView.body should include("Testing App")
        appView.body should include("Pending Gatekeeper Approval App")
        appView.body should include("Pending Requester Verification App")
        appView.body should include("Production App")

        appView.body should include("Created")
        appView.body should include("Pending gatekeeper check")
        appView.body should include("Pending submitter verification")
        appView.body should include("Active")
      }

      "Display filter by status entries in correct order" in {
        val appView = applicationView()

        val document = Jsoup.parse(appView.body)
        val status = document.select(s"#status")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Created"
        status.get(0).child(2).text() shouldBe "Pending gatekeeper check"
        status.get(0).child(3).text() shouldBe "Pending submitter verification"
        status.get(0).child(4).text() shouldBe "Active"
      }

      "Terms of Use status filter entries in correct order" in {
        val appView = applicationView()

        val document = Jsoup.parse(appView.body)
        val status = document.select(s"#tou_status")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Not agreed"
        status.get(0).child(2).text() shouldBe "Agreed"
      }

      "Access type filter entries in correct order" in {
        val appView = applicationView()

        val document = Jsoup.parse(appView.body)
        val status = document.select(s"#access_type")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Standard"
        status.get(0).child(2).text() shouldBe "ROPC"
        status.get(0).child(3).text() shouldBe "Privileged"
      }
    }

    "Called by a superuser" should {

      "Display the 'Add privileged or ROPC application' button" in {
        val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, Map.empty, true)

        val appView = applicationView()

        appView.body should include("""Add privileged or ROPC application""")
      }
    }

    "Called by a non-superuser" should {

      "Not display the 'Add privileged or ROPC application' button" in {
        val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, Map.empty, false)

        val appView = applicationView()

        appView.body shouldNot include("""Add privileged or ROPC application""")
      }
    }
  }
}
