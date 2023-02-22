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

package uk.gov.hmrc.gatekeeper.views.developers

import org.jsoup.Jsoup

import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.apicataloguepublish.PublishTemplate

class PublishTemplateSpec extends CommonViewSpec {

  sealed case class TestApplication(
      name: String,
      collaborators: Set[Collaborator],
      id: ApplicationId = ApplicationId.random,
      state: ApplicationState = ApplicationState(State.PRODUCTION),
      clientId: ClientId = ClientId("a-client-id"),
      deployedTo: String = "PRODUCTION"
    ) extends Application

  def admin(email: String) = Collaborator(email, CollaboratorRole.ADMINISTRATOR, UserId.random)

  "Publish Template" should {
    implicit val request  = FakeRequest().withCSRFToken
    implicit val userName = LoggedInUser(Some("gate.keeper"))
    implicit val messages = app.injector.instanceOf[MessagesControllerComponents].messagesApi.preferred(request)

    val publishTemplate = app.injector.instanceOf[PublishTemplate]

    "show the publish all and publish one buttons" in {

      val document = Jsoup.parse(publishTemplate("page title", "heading", "message").body)

      document.getElementById("heading").text() shouldBe "heading"
      document.getElementById("message").text() shouldBe "message"
      document.getElementById("publish-all-form").attr("action") shouldBe "/api-gatekeeper/apicatalogue/publishall"
      document.getElementById("publish-one-form").attr("action") shouldBe "/api-gatekeeper/apicatalogue/publish?serviceName="
      elementExistsById(document, "publish-all-button") shouldBe true
      elementExistsById(document, "publish-one-button") shouldBe true
      elementExistsById(document, "publish-one-label") shouldBe true
      elementExistsById(document, "publish-one-input") shouldBe true

    }
  }

}
