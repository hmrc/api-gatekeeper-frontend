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

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.mocks.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.models.organisations.{OrganisationId, OrganisationWithApps}
import uk.gov.hmrc.gatekeeper.views.html.applications.OrganisationView

class OrganisationViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val organisationView = app.injector.instanceOf[OrganisationView]

    implicit val mockConfig: AppConfig      = mock[AppConfig]
    implicit val loggedInUser: LoggedInUser = LoggedInUser(Some("Bob Dole"))

    val applicationResponse  = ApplicationResponseBuilder.buildApplication(ApplicationId.random, ClientId.random, UserId.random)
    val organisationName     = "Organisation Name"
    val organisationId       = OrganisationId("1")
    val organisationWithApps = OrganisationWithApps(organisationId, organisationName, List(applicationResponse))

    val getApprovalsUrl = (appId: ApplicationId, deployedTo: Environment) => "approvals/url"

    val organisationViewWithApplication: () => HtmlFormat.Appendable =
      () => organisationView(organisationWithApps, getApprovalsUrl, Map.empty, Map.empty)

    val organisationViewWithNoApplication: () => HtmlFormat.Appendable =
      () => organisationView(organisationWithApps.copy(applications = List.empty), getApprovalsUrl, Map.empty, Map.empty)
  }

  "OrganisationView" should {

    "Display the subscription filters" in new Setup {
      organisationViewWithApplication().body should include("<option selected value>All applications</option>")
      organisationViewWithApplication().body should include("""<option  value="ANY">One or more subscriptions</option>""")
      organisationViewWithApplication().body should include("""<option  value="NONE">No subscriptions</option>""")
    }

    "Display an application" in new Setup {
      organisationViewWithApplication().body should include(organisationName)
      organisationViewWithApplication().body should include(applicationResponse.name.toString())
      organisationViewWithApplication().body should include("23 Dec 2022")
      organisationViewWithApplication().body should include("02 Oct 2023")
      organisationViewWithApplication().body should include("Created")
    }
    "Display no application" in new Setup {
      organisationViewWithNoApplication().body should include(organisationName)
      organisationViewWithNoApplication().body should include("No applications")
    }

  }
}
