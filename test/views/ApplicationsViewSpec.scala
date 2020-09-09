/*
 * Copyright 2020 HM Revenue & Customs
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

package views

import config.AppConfig
import model.APIStatus._
import model.{LoggedInUser, _}
import org.joda.time.DateTime
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.html.applications.ApplicationsView

class ApplicationsViewSpec extends CommonViewSpec {

  trait Setup {
    val applicationsView = app.injector.instanceOf[ApplicationsView]

    implicit val mockConfig: AppConfig = mock[AppConfig]
    implicit val loggedInUser = LoggedInUser(Some("Bob Dole"))

    val apis = Map[String, Seq[VersionSummary]](
      displayedStatus(STABLE) -> Seq(VersionSummary("Dummy API", STABLE, APIIdentifier(ApiContext("dummy-api"), "1.0"))),
      displayedStatus(BETA) -> Seq(VersionSummary("Beta API", BETA, APIIdentifier(ApiContext("beta-api"), "1.0"))),
      displayedStatus(RETIRED) -> Seq(VersionSummary("Retired API", RETIRED, APIIdentifier(ApiContext("ret-api"), "1.0"))),
      displayedStatus(DEPRECATED) -> Seq(VersionSummary("Deprecated API", DEPRECATED, APIIdentifier(ApiContext("dep-api"), "1.0")))
    )
    val applications = Seq[ApplicationResponse](
      ApplicationResponse(ApplicationId.random, "Testing App", Some("Testing App"), Set.empty, DateTime.now(), ApplicationState(State.TESTING), Standard(), Seq.empty, termsOfUseAgreed = true, deployedTo = "PRODUCTION"),
      ApplicationResponse(ApplicationId.random, "Pending Gatekeeper Approval App", Some("Pending Gatekeeper Approval App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_GATEKEEPER_APPROVAL), Ropc(), Seq.empty, termsOfUseAgreed = true, deployedTo = "PRODUCTION"),
      ApplicationResponse(ApplicationId.random, "Pending Requester Verification App", Some("Pending Requester Verification App"), Set.empty, DateTime.now(), ApplicationState(State.PENDING_REQUESTER_VERIFICATION), Privileged(), Seq.empty, termsOfUseAgreed = true, deployedTo = "PRODUCTION"),
      ApplicationResponse(ApplicationId.random, "Production App", Some("Production App"), Set.empty, DateTime.now(), ApplicationState(State.PRODUCTION), Standard(), Seq.empty, termsOfUseAgreed = true, deployedTo = "PRODUCTION")
    )
    val applicationViewWithNoApis: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0), Map.empty, false, Map.empty)
    val applicationViewWithApis: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(Seq.empty, 0, 0, 0, 0), apis, false, Map.empty)
    val applicationViewWithApplication: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(applications, 1, 4, 4, 4), Map.empty, false, Map.empty)
    val applicationViewWithApplicationDocument = Jsoup.parse(applicationViewWithApplication().body)
  }

  "ApplicationsView" when {

    "Called with no APIs" should {

      "Display only subscription filters" in new Setup {
        applicationViewWithNoApis().body must include("<option selected value>All applications</option>")
        applicationViewWithNoApis().body must include("""<option  value="ANY">One or more subscriptions</option>""")
        applicationViewWithNoApis().body must include("""<option  value="NONE">No subscriptions</option>""")
      }

      "Not include application state filters" in new Setup {
        applicationViewWithNoApis().body must not include "Stable"
        applicationViewWithNoApis().body must not include "Beta"
        applicationViewWithNoApis().body must not include "Retired"
        applicationViewWithNoApis().body must not include "Deprecated"
      }

      "Display the Terms of Use filters" in new Setup {
        applicationViewWithNoApis().body must include("""<option selected id="default-tou-status" value>All</option>""")
        applicationViewWithNoApis().body must include("""<option  value="NOT_ACCEPTED">Not agreed</option>""")
        applicationViewWithNoApis().body must include("""<option  value="ACCEPTED">Agreed</option>""")
      }
    }

    "Called with APIs" should {
      "Display the subscription filters" in new Setup {
        applicationViewWithApis().body must include("<option selected value>All applications</option>")
        applicationViewWithApis().body must include("""<option  value="ANY">One or more subscriptions</option>""")
        applicationViewWithApis().body must include("""<option  value="NONE">No subscriptions</option>""")
      }

      "Include the application state filters" in new Setup {
        applicationViewWithApis().body must include ("Stable")
        applicationViewWithApis().body must include ("Beta")
        applicationViewWithApis().body must include ("Retired")
        applicationViewWithApis().body must include ("Deprecated")
      }
    }

    "Called with application" should {
      "Display all four applications in all four states" in new Setup {
        applicationViewWithApplication().body must include("Testing App")
        applicationViewWithApplication().body must include("Pending Gatekeeper Approval App")
        applicationViewWithApplication().body must include("Pending Requester Verification App")
        applicationViewWithApplication().body must include("Production App")

        applicationViewWithApplication().body must include("Created")
        applicationViewWithApplication().body must include("Pending gatekeeper check")
        applicationViewWithApplication().body must include("Pending submitter verification")
        applicationViewWithApplication().body must include("Active")
      }

      "Display filter by status entries in correct order" in new Setup {

        val status = applicationViewWithApplicationDocument.select(s"#status")

        status.get(0).child(0).text() mustBe "All"
        status.get(0).child(1).text() mustBe "Created"
        status.get(0).child(2).text() mustBe "Pending gatekeeper check"
        status.get(0).child(3).text() mustBe "Pending submitter verification"
        status.get(0).child(4).text() mustBe "Active"
      }

      "Terms of Use status filter entries in correct order" in new Setup {
        val status = applicationViewWithApplicationDocument.select(s"#tou_status")

        status.get(0).child(0).text() mustBe "All"
        status.get(0).child(1).text() mustBe "Not agreed"
        status.get(0).child(2).text() mustBe "Agreed"
      }

      "Access type filter entries in correct order" in new Setup {
        val status = applicationViewWithApplicationDocument.select(s"#access_type")

        status.get(0).child(0).text() mustBe "All"
        status.get(0).child(1).text() mustBe "Standard"
        status.get(0).child(2).text() mustBe "ROPC"
        status.get(0).child(3).text() mustBe "Privileged"
      }
    }

    "Called by a superuser" should {

      "Display the 'Add privileged or ROPC application' button" in new Setup {
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedSubscribedApplicationResponse(Seq.empty, 0, 0, 0, 0), Map.empty, true, Map.empty)
        applicationView().body must include("""Add privileged or ROPC application""")
      }
    }

    "Called by a non-superuser" should {

      "Not display the 'Add privileged or ROPC application' button" in new Setup {
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedSubscribedApplicationResponse(Seq.empty, 0, 0, 0, 0), Map.empty, false, Map.empty)
        applicationView().body mustNot include("""Add privileged or ROPC application""")
      }
    }
  }
}
