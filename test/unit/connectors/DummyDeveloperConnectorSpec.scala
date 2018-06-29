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

package unit.connectors

import connectors.DummyDeveloperConnector
import model._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterEach, Matchers}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}


class DummyDeveloperConnectorSpec extends UnitSpec with Matchers with ScalaFutures with WiremockSugar with BeforeAndAfterEach with WithFakeApplication {

  implicit val hc = HeaderCarrier()

  "fetchByEmail" should {
    "return an UnregisteredCollaborator" in {
      await(DummyDeveloperConnector.fetchByEmail("user@example.com")) shouldBe UnregisteredCollaborator("user@example.com")
    }
  }

  "fetchByEmails" should {
    "return an empty sequence" in {
      await(DummyDeveloperConnector.fetchByEmails(Seq("user@example.com"))) shouldBe Seq.empty
    }
  }

  "fetchAll" should {
    "return an empty sequence" in {
      await(DummyDeveloperConnector.fetchAll()) shouldBe Seq.empty
    }
  }

  "deleteDeveloper" should {
    "return a success result" in {
      await(DummyDeveloperConnector.deleteDeveloper(DeleteDeveloperRequest("gate.keeper", "user@example.com"))) shouldBe  DeveloperDeleteSuccessResult
    }
  }
}
