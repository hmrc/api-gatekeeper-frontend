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

package unit.model

import java.util.UUID

import model.CollaboratorRole._
import model.SubscribedApplicationResponse._
import model._
import org.joda.time.DateTime
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class SubscribedApplicationResponseSpec extends UnitSpec with Matchers {

  def randomText = UUID.randomUUID().toString

  "SubscribedApplicationResponse" should {
    val id = UUID.randomUUID()
    val name = randomText
    val description = randomText
    val collaborator = Collaborator(randomText, DEVELOPER)
    val createdOn = DateTime.now
    val state = ApplicationState()
    val sub = SubscriptionNameAndVersion("subName", "subVersion")
    val appResponse = ApplicationResponse(id, name, randomText, Some(description), Set(collaborator), createdOn, Standard(), state)

    "create from ApplicationResponse" in {
      val expected = SubscribedApplicationResponse(id, name, Some(description), Set(collaborator), createdOn, state, Seq(sub),
        termsOfUseAgreed = false)

      createFrom(appResponse, Seq(sub)) shouldBe expected
    }
    "identify terms of use accepted" in {
      val appResponseToUAgreed = appResponse.copy(checkInformation = Some(CheckInformation(termsOfUseAgreements = Seq(TermsOfUseAgreement("email", createdOn, "1.0")))))

      val expected = SubscribedApplicationResponse(id, name, Some(description), Set(collaborator), createdOn, state, Seq(sub),
        termsOfUseAgreed = true)

      createFrom(appResponseToUAgreed, Seq(sub)) shouldBe expected
    }
  }
}