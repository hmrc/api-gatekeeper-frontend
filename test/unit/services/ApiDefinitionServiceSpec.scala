/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.services

import connectors._
import model.{APIAccess, APIAccessType, APIDefinition, APIStatus, APIVersion}
import org.mockito.BDDMockito._
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import services.ApiDefinitionService
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

class ApiDefinitionServiceSpec extends UnitSpec with Matchers with MockitoSugar {
  implicit val hc: HeaderCarrier = new HeaderCarrier
  val mockSandboxApiDefinitionConnector = mock[SandboxApiDefinitionConnector]
  val mockProductionApiDefinitionConnector = mock[ProductionApiDefinitionConnector]

  val definitionService = new ApiDefinitionService(mockSandboxApiDefinitionConnector, mockProductionApiDefinitionConnector)

  val publicDefinition = APIDefinition(
    "publicAPI", "http://localhost/",
    "publicAPI", "public api.", "public-api",
    Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PUBLIC)))), Some(false)
  )

  val privateDefinition = APIDefinition(
    "privateAPI", "http://localhost/",
    "privateAPI", "private api.", "private-api",
    Seq(APIVersion("1.0", APIStatus.STABLE, Some(APIAccess(APIAccessType.PRIVATE)))), Some(false)
  )

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs" in {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition)))

        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition)))

        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq.empty))

        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq.empty))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Include no duplicates" in {

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition, publicDefinition)))

        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition, privateDefinition)))

        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition, publicDefinition)))

        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition, privateDefinition)))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions

        await(allDefinitions) should have size 2
      }
    }
  }
}
