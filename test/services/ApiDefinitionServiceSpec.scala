/*
 * Copyright 2020 HM Revenue & Customs
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

package services

import connectors._
import model.Environment._
import model._
import org.mockito.BDDMockito._
import org.mockito.Mockito.{never, verify}
import org.scalatest.Matchers
import org.scalatest.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiDefinitionServiceSpec extends UnitSpec with Matchers with MockitoSugar {
  trait Setup {
    implicit val hc: HeaderCarrier = new HeaderCarrier
    val mockSandboxApiDefinitionConnector = mock[SandboxApiDefinitionConnector]
    val mockProductionApiDefinitionConnector = mock[ProductionApiDefinitionConnector]

    given(mockSandboxApiDefinitionConnector.environment).willReturn(Environment.SANDBOX)
    given(mockProductionApiDefinitionConnector.environment).willReturn(Environment.PRODUCTION)

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
  }

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs in both environments" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq.empty))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq.empty))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Return a combination of public and private APIs in sandbox" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition)))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions(Some(SANDBOX))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector, never).fetchPublic()
        verify(mockProductionApiDefinitionConnector, never).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector).fetchPublic()
        verify(mockSandboxApiDefinitionConnector).fetchPrivate()
      }

      "Return a combination of public and private APIs in production" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition)))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions(Some(PRODUCTION))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector).fetchPublic()
        verify(mockProductionApiDefinitionConnector).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector, never).fetchPublic()
        verify(mockSandboxApiDefinitionConnector, never).fetchPrivate()
      }

      "Include no duplicates" in new Setup {

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition, publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition, privateDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition, publicDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition, privateDefinition)))

        val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) should have size 2
      }
    }
  }

  "apis" when {
    "get all apis" in new Setup {

      val publicSandbox = publicDefinition.copy(name="sandbox-public")
      val privateSandbox = privateDefinition.copy(name="sandbox-private")

      given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicDefinition)))
      given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateDefinition)))
      given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(publicSandbox)))
      given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(privateSandbox)))

      val allDefinitions: Seq[(APIDefinition, Environment)] = await(definitionService.apis)

      allDefinitions shouldBe Seq(
        (privateDefinition, Environment.PRODUCTION),
        (publicDefinition, Environment.PRODUCTION),
        (privateSandbox, Environment.SANDBOX),
        (publicSandbox, Environment.SANDBOX)
      )
    }
  }
}
