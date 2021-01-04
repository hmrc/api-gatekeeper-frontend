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

package connectors

import model._
import org.scalatest.concurrent.ScalaFutures
import org.mockito.{MockitoSugar, ArgumentMatchersSugar}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class DummyDeveloperConnectorSpec extends UnitSpec with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication with MockitoSugar with ArgumentMatchersSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()
  val email: String = "user@example.com"
  val loggedInUser: String = "admin-user"

  val underTest = new DummyDeveloperConnector

  "fetchByEmail" should {
    "return an UnregisteredCollaborator" in {
      await(underTest.fetchByEmail(email)) shouldBe a[User]
    }
  }

  "fetchByEmails" should {
    "return an empty sequence" in {
      await(underTest.fetchByEmails(Seq(email))) shouldBe Seq.empty
    }
  }

  "fetchByEMailPreferences" should {
    "return an empty sequence" in {
      await(underTest.fetchByEmailPreferences(TopicOptionChoice.TECHNICAL)) shouldBe Seq.empty
    }
  }

  "fetchAll" should {
    "return an empty sequence" in {
      await(underTest.fetchAll()) shouldBe Seq.empty
    }
  }

  "deleteDeveloper" should {
    "return a success result" in {
      await(underTest.deleteDeveloper(DeleteDeveloperRequest("gate.keeper", email))) shouldBe  DeveloperDeleteSuccessResult
    }
  }

  "removeMfa" should {
    "return an UnregisteredCollaborator" in {
      await(underTest.removeMfa(email, loggedInUser)) shouldBe a[User]
    }
  }
}
