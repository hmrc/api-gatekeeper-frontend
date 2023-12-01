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

package uk.gov.hmrc.gatekeeper.views

import java.time.{LocalDateTime, Period}

import org.jsoup.Jsoup

import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiStatus
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, ApplicationState, IpAllowlist, MoreApplication}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators, GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.views.html.applications.ApplicationsView

class ApplicationsViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val applicationsView = app.injector.instanceOf[ApplicationsView]

    implicit val mockConfig: AppConfig = mock[AppConfig]
    implicit val loggedInUser          = LoggedInUser(Some("Bob Dole"))

    val apis = Map[String, Seq[VersionSummary]](
      ApiStatus.STABLE.displayText     -> Seq(VersionSummary("Dummy API", ApiStatus.STABLE, ApiIdentifier(ApiContext("dummy-api"), ApiVersionNbr.random))),
      ApiStatus.BETA.displayText       -> Seq(VersionSummary("Beta API", ApiStatus.BETA, ApiIdentifier(ApiContext("beta-api"), ApiVersionNbr.random))),
      ApiStatus.RETIRED.displayText    -> Seq(VersionSummary("Retired API", ApiStatus.RETIRED, ApiIdentifier(ApiContext("ret-api"), ApiVersionNbr.random))),
      ApiStatus.DEPRECATED.displayText -> Seq(VersionSummary("Deprecated API", ApiStatus.DEPRECATED, ApiIdentifier(ApiContext("dep-api"), ApiVersionNbr.random)))
    )

    val collaborators: Set[Collaborator] = Set(
      Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
    )

    val applications    = List[ApplicationResponse](
      buildApplication(
        ApplicationId.random,
        ClientId("clientid1"),
        "gatewayId1",
        Some("Testing App"),
        Environment.PRODUCTION,
        Some("Testing App"),
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = LocalDateTime.now())
      ),
      buildApplication(
        ApplicationId.random,
        ClientId("clientid1"),
        "gatewayId1",
        Some("Pending Gatekeeper Approval App"),
        Environment.PRODUCTION,
        Some("Pending Gatekeeper Approval App"),
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = LocalDateTime.now())
      ),
      buildApplication(
        ApplicationId.random,
        ClientId("clientid1"),
        "gatewayId1",
        Some("Pending Requester Verification App"),
        Environment.PRODUCTION,
        Some("Pending Requester Verification App"),
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = LocalDateTime.now())
      ),
      buildApplication(
        ApplicationId.random,
        ClientId("clientid1"),
        "gatewayId1",
        Some("Production App"),
        Environment.PRODUCTION,
        Some("Production App"),
        collaborators,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = LocalDateTime.now())
      )
    )
    val getApprovalsUrl = (appId: ApplicationId, deployedTo: Environment) => "approvals/url"

    val applicationViewWithNoApis: () => HtmlFormat.Appendable =
      () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, false, Map.empty, getApprovalsUrl)
    val applicationViewWithApis: () => HtmlFormat.Appendable   = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), apis, false, Map.empty, getApprovalsUrl)

    val applicationViewWithApplication: () => HtmlFormat.Appendable =
      () => applicationsView(PaginatedApplicationResponse(applications, 1, 4, 4, 4), Map.empty, false, Map.empty, getApprovalsUrl)
    val applicationViewWithApplicationDocument                      = Jsoup.parse(applicationViewWithApplication().body)
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
    }

    "Called with APIs" should {
      "Display the subscription filters" in new Setup {
        applicationViewWithApis().body should include("<option selected value>All applications</option>")
        applicationViewWithApis().body should include("""<option  value="ANY">One or more subscriptions</option>""")
        applicationViewWithApis().body should include("""<option  value="NONE">No subscriptions</option>""")
      }

      "Include the application state filters" in new Setup {
        applicationViewWithApis().body should include("Stable")
        applicationViewWithApis().body should include("Beta")
        applicationViewWithApis().body should include("Retired")
        applicationViewWithApis().body should include("Deprecated")
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

        status.get(0).child(0).text() shouldBe "All - Excluding deleted"
        status.get(0).child(1).text() shouldBe "All"
        status.get(0).child(2).text() shouldBe "Created"
        status.get(0).child(3).text() shouldBe "Pending responsible individual verification"
        status.get(0).child(4).text() shouldBe "Pending gatekeeper check"
        status.get(0).child(5).text() shouldBe "Pending submitter verification"
        status.get(0).child(6).text() shouldBe "Active"
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
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, true, Map.empty, getApprovalsUrl)
        applicationView().body should include("""Add privileged or ROPC application""")
      }
    }

    "Called by a non-superuser" should {

      "Not display the 'Add privileged or ROPC application' button" in new Setup {
        val applicationView: () => HtmlFormat.Appendable = () => applicationsView(PaginatedApplicationResponse(List.empty, 0, 0, 0, 0), Map.empty, false, Map.empty, getApprovalsUrl)
        applicationView().body shouldNot include("""Add privileged or ROPC application""")
      }
    }
  }
}
