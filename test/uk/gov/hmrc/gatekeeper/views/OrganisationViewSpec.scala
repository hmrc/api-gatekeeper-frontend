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

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.views.html.applications.OrganisationView

import java.time.{Instant, LocalDateTime}

class OrganisationViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val organisationView = app.injector.instanceOf[OrganisationView]

    implicit val mockConfig: AppConfig      = mock[AppConfig]
    implicit val loggedInUser: LoggedInUser = LoggedInUser(Some("Bob Dole"))

    val collaborators: Set[Collaborator] = Set(
      Collaborators.Administrator(UserId.random, "sample@example.com".toLaxEmail),
      Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
    )

    val applications    = List[GKApplicationResponse](
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
        state = ApplicationState(updatedOn = Instant.now())
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
        state = ApplicationState(updatedOn = Instant.now())
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
        state = ApplicationState(updatedOn = Instant.now())
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
        state = ApplicationState(updatedOn = Instant.now())
      )
    )
    val getApprovalsUrl = (appId: ApplicationId, deployedTo: Environment) => "approvals/url"

    val organisationViewWithApplication: () => HtmlFormat.Appendable =
      () => organisationView(PaginatedApplicationResponse(applications, 1, 4, 4, 4), false, Map.empty, getApprovalsUrl)
  }

  "ApplicationsView" when {

    "Called with application" should {
      "Display all four applications in all four states" in new Setup {
        organisationViewWithApplication().body should include("Testing App")
        organisationViewWithApplication().body should include("Pending Gatekeeper Approval App")
        organisationViewWithApplication().body should include("Pending Requester Verification App")
        organisationViewWithApplication().body should include("Production App")

        organisationViewWithApplication().body should include("Created")
//        applicationViewWithApplication().body should include("Pending gatekeeper check")
//        applicationViewWithApplication().body should include("Pending submitter verification")
//        applicationViewWithApplication().body should include("Active")
      }
    }
  }
}
