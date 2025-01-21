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

import java.time.Instant

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat.Appendable

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, ApplicationWithCollaborators, Collaborators, MoreApplication}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.applications.ApplicationProtectedFromDeletionView

class ApplicationProtectedFromDeletionViewSpec extends CommonViewSpec {

  trait Setup extends ApplicationBuilder {
    val request                                                                    = FakeRequest().withCSRFToken
    val applicationProtectedFromDeletionView: ApplicationProtectedFromDeletionView = app.injector.instanceOf[ApplicationProtectedFromDeletionView]

    val application: ApplicationWithCollaborators =
      buildApplication(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        Some("application1"),
        Environment.PRODUCTION,
        None,
        Set(
          Collaborators.Administrator(UserId.random, LaxEmailAddress("sample@example.com")),
          Collaborators.Developer(UserId.random, LaxEmailAddress("someone@example.com"))
        ),
        Instant.now(),
        Some(Instant.now()),
        access = Access.Standard(),
        state = ApplicationState(updatedOn = Instant.now()),
        moreApplication = MoreApplication(allowAutoDelete = false),
        deleteRestriction = aDeleteRestriction
      )

    val reason = "Do not delete this application"
  }

  "Delete Restriction Enabled view" should {
    "show reason, date and radio buttons when deletion restriction is enabled for application" in new Setup {
      val result: Appendable =
        applicationProtectedFromDeletionView(application, reason)(request, LoggedInUser(None), messagesProvider)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      elementExistsByText(document, "h1", "Application cannot be deleted") shouldBe true
      elementExistsByText(document, "p", s"You cannot delete ${application.name.value} because:") shouldBe true
      elementExistsByText(document, "dd", reason)
    }
  }
}
