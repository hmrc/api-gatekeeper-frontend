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
      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, Map.empty)

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
    }

    "Called with APIs" should {

      val apis = Map[String, Seq[VersionSummary]](
        displayedStatus(STABLE) -> Seq(VersionSummary("Dummy API", STABLE, APIIdentifier("dummy-api", "1.0"))),
        displayedStatus(BETA) -> Seq(VersionSummary("Beta API", BETA, APIIdentifier("beta-api", "1.0"))),
        displayedStatus(RETIRED) -> Seq(VersionSummary("Retired API", RETIRED, APIIdentifier("ret-api", "1.0"))),
        displayedStatus(DEPRECATED) -> Seq(VersionSummary("Deprecated API", DEPRECATED, APIIdentifier("dep-api", "1.0")))
      )

      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(Seq.empty, apis)

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
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Testing App", Some("Testing App"), Set.empty, DateTime.now(), ApplicationState(State.TESTING), Seq.empty),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Pending Gatekeeper Approval App", Some("Pending Gatekeeper Approval App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_GATEKEEPER_APPROVAL), Seq.empty),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Pending Requester Verification App", Some("Pending Requester Verification App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_REQUESTER_VERIFICATION), Seq.empty),
        DetailedSubscribedApplicationResponse(UUID.randomUUID(), "Production App", Some("Production App"), Set.empty, DateTime.now(), ApplicationState(State.PRODUCTION), Seq.empty)
      )

      val applicationView: () => HtmlFormat.Appendable = () => html.applications.applications(applications, Map.empty)

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
    }
  }
}
