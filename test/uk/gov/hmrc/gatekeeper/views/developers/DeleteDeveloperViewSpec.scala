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

import java.time.{Instant, LocalDateTime}

import org.jsoup.Jsoup

import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents}
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, Collaborators, State}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.developers.DeleteDeveloperView
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock

class DeleteDeveloperViewSpec extends CommonViewSpec with ApplicationWithCollaboratorsFixtures with FixedClock {

  def admin(email: LaxEmailAddress) = Collaborators.Administrator(UserId.random, email)

  "delete developer view" should {
    // implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken
    implicit val userName = LoggedInUser(Some("gate.keeper"))
    implicit val messages = app.injector.instanceOf[MessagesControllerComponents].messagesApi.preferred(fakeRequest)

    val deleteDeveloper = app.injector.instanceOf[DeleteDeveloperView]

    "show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app       = standardApp.withCollaborators(
        admin(LaxEmailAddress("email@example.com")), 
        admin(LaxEmailAddress("other@example.com"))
      )
      val developer = Developer(RegisteredUser(LaxEmailAddress("email@example.com"), UserId.random, "firstname", "lastName", false), List(app))

      val document = Jsoup.parse(deleteDeveloper(developer).body)
      elementExistsById(document, "submit") shouldBe true
      elementExistsById(document, "cancel") shouldBe true
      elementExistsById(document, "finish") shouldBe false
    }

    "not show the controls to delete the developer when the developer has no apps that they are the sole admin on" in {
      val app       = standardApp.withCollaborators(admin(LaxEmailAddress("email@example.com")))
      val developer = Developer(RegisteredUser(LaxEmailAddress("email@example.com"), UserId.random, "firstname", "lastName", false), List(app))

      val document = Jsoup.parse(deleteDeveloper(developer).body)
      elementExistsById(document, "submit") shouldBe false
      elementExistsById(document, "cancel") shouldBe false
      elementExistsById(document, "finish") shouldBe true
    }
  }
}
