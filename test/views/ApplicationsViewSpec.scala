/*
 * Copyright 2021 HM Revenue & Customs
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
import model.ApiStatus._
import model.{LoggedInUser, _}
import org.jsoup.Jsoup
import play.twirl.api.HtmlFormat
import views.html.applications.ApplicationsView
import org.joda.time.DateTime

class ApplicationsViewSpec extends CommonViewSpec {

  trait Setup {
    val applicationsView = app.injector.instanceOf[ApplicationsView]

    implicit val mockConfig: AppConfig = mock[AppConfig]
    implicit val loggedInUser = LoggedInUser(Some("Bob Dole"))

    val apis = Map[String, Seq[VersionSummary]](
      displayedStatus(STABLE) -> Seq(VersionSummary("Dummy API", STABLE, ApiIdentifier(ApiContext("dummy-api"), ApiVersion.random))),
      displayedStatus(BETA) -> Seq(VersionSummary("Beta API", BETA, ApiIdentifier(ApiContext("beta-api"), ApiVersion.random))),
      displayedStatus(RETIRED) -> Seq(VersionSummary("Retired API", RETIRED, ApiIdentifier(ApiContext("ret-api"), ApiVersion.random))),
      displayedStatus(DEPRECATED) -> Seq(VersionSummary("Deprecated API", DEPRECATED, ApiIdentifier(ApiContext("dep-api"), ApiVersion.random)))
    )

    val collaborators = Set(
      Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random),
      Collaborator("someone@example.com", CollaboratorRole.DEVELOPER, UserId.random))

    val applications = List[ApplicationResponse](
      ApplicationResponse(ApplicationId.random, ClientId("clientid1"), "gatewayId1", "Testing App", "PRODUCTION", Some("Testing App"), collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()),
      ApplicationResponse(ApplicationId.random, ClientId("clientid1"), "gatewayId1", "Pending Gatekeeper Approval App", "PRODUCTION", Some("Pending Gatekeeper Approval App"), collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()),
      ApplicationResponse(ApplicationId.random, ClientId("clientid1"), "gatewayId1", "Pending Requester Verification App", "PRODUCTION", Some("Pending Requester Verification App"), collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState()),
      ApplicationResponse(ApplicationId.random, ClientId("clientid1"), "gatewayId1", "Production App", "PRODUCTION", Some("Production App"), collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
    )
    val applicationViewWithNoApis: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, false, Map.empty)
    val applicationViewWithApis: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), apis, false, Map.empty)
    val applicationViewWithApplication: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(applications, 1, 4, 4, 4), Map.empty, false, Map.empty)
    val applicationViewWithApplicationDocument = Jsoup.parse(applicationViewWithApplication().body)
  }

  "ApplicationsView" when {

    "Called with no APIs" should {

      "Display only subscription filters" in new Setup {
        applicationViewWithNoApis().body should include("<option selected value>All applications</option>")
        applicationViewWithNoApis().body should include("""<option  value="ANY">One or more subscriptions</option>""")
        applicationViewWithNoApis().body should include("""<option  value="NONE">No subscriptions</option>""")
      }

      "Not include application state filters" in new Setup {
        applicationViewWithNoApis().body should not include "Stable"
        applicationViewWithNoApis().body should not include "Beta"
        applicationViewWithNoApis().body should not include "Retired"
        applicationViewWithNoApis().body should not include "Deprecated"
      }

      "Display the Terms of Use filters" in new Setup {
        applicationViewWithNoApis().body should include("""<option selected id="default-tou-status" value>All</option>""")
        applicationViewWithNoApis().body should include("""<option  value="NOT_ACCEPTED">Not agreed</option>""")
        applicationViewWithNoApis().body should include("""<option  value="ACCEPTED">Agreed</option>""")
      }
    }

    "Called with APIs" should {
      "Display the subscription filters" in new Setup {
        applicationViewWithApis().body should include("<option selected value>All applications</option>")
        applicationViewWithApis().body should include("""<option  value="ANY">One or more subscriptions</option>""")
        applicationViewWithApis().body should include("""<option  value="NONE">No subscriptions</option>""")
      }

      "Include the application state filters" in new Setup {
        applicationViewWithApis().body should include ("Stable")
        applicationViewWithApis().body should include ("Beta")
        applicationViewWithApis().body should include ("Retired")
        applicationViewWithApis().body should include ("Deprecated")
      }
    }

    "Called with application" should {
      "Display all four applications in all four states" in new Setup {
        applicationViewWithApplication().body should include("Testing App")
        applicationViewWithApplication().body should include("Pending Gatekeeper Approval App")
        applicationViewWithApplication().body should include("Pending Requester Verification App")
        applicationViewWithApplication().body should include("Production App")

        applicationViewWithApplication().body should include("Created")
        applicationViewWithApplication().body should include("Pending gatekeeper check")
        applicationViewWithApplication().body should include("Pending submitter verification")
        applicationViewWithApplication().body should include("Active")
      }

      "Display filter by status entries in correct order" in new Setup {

        val status = applicationViewWithApplicationDocument.select(s"#status")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Created"
        status.get(0).child(2).text() shouldBe "Pending gatekeeper check"
        status.get(0).child(3).text() shouldBe "Pending submitter verification"
        status.get(0).child(4).text() shouldBe "Active"
      }

      "Terms of Use status filter entries in correct order" in new Setup {
        val status = applicationViewWithApplicationDocument.select(s"#tou_status")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Not agreed"
        status.get(0).child(2).text() shouldBe "Agreed"
      }

      "Access type filter entries in correct order" in new Setup {
        val status = applicationViewWithApplicationDocument.select(s"#access_type")

        status.get(0).child(0).text() shouldBe "All"
        status.get(0).child(1).text() shouldBe "Standard"
        status.get(0).child(2).text() shouldBe "ROPC"
        status.get(0).child(3).text() shouldBe "Privileged"
      }
    }

    "Called by a superuser" should {

      "Display the 'Add privileged or ROPC application' button" in new Setup {
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, true, Map.empty)
        applicationView().body should include("""Add privileged or ROPC application""")
      }
    }

    "Called by a non-superuser" should {

      "Not display the 'Add privileged or ROPC application' button" in new Setup {
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, false, Map.empty)
        applicationView().body shouldNot include("""Add privileged or ROPC application""")
      }
    }
  }
}
