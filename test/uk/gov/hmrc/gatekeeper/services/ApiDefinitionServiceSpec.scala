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

package uk.gov.hmrc.gatekeeper.services

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import mocks.connectors.ApiDefinitionConnectorMockProvider
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models._

class ApiDefinitionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApiDefinitionConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val definitionService = new ApiDefinitionService(mockSandboxApiDefinitionConnector, mockProductionApiDefinitionConnector)

    val publicDefinition = ApiDefinitionGK(
      "publicAPI",
      "http://localhost/",
      "publicAPI",
      "public api.",
      ApiContext.random,
      List(ApiVersionGK(ApiVersionNbr.random, ApiVersionSource.UNKNOWN, ApiStatus.STABLE, Some(ApiAccess.PUBLIC))),
      Some(false),
      None
    )

    val privateDefinition = ApiDefinitionGK(
      "privateAPI",
      "http://localhost/",
      "privateAPI",
      "private api.",
      ApiContext.random,
      List(ApiVersionGK(ApiVersionNbr.random, ApiVersionSource.UNKNOWN, ApiStatus.STABLE, Some(ApiAccess.Private(Nil,None)))),
      Some(false),
      None
    )

    val version1 = ApiVersionGK(ApiVersionNbr("1.0"), ApiVersionSource.UNKNOWN, ApiStatus.BETA, Some(ApiAccess.PUBLIC))
    val version2 = ApiVersionGK(ApiVersionNbr("2.0"), ApiVersionSource.UNKNOWN, ApiStatus.BETA, Some(ApiAccess.Private(Nil,None)))
    val version3 = ApiVersionGK(ApiVersionNbr("3.0"), ApiVersionSource.UNKNOWN, ApiStatus.BETA, Some(ApiAccess.Private(Nil,None)))

    val customsDeclarations1 = ApiDefinitionGK(
      serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = List(version1),
      categories = Some(List(ApiCategory("CUSTOMS")))
    )

    val customsDeclarations2 = ApiDefinitionGK(
      serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = List(version2.copy(), version3.copy()),
      categories = Some(List(ApiCategory("CUSTOMS")))
    )
  }

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs in both environments" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApiDefinitionConnectorMock.Prod.FetchPublic.returns(publicDefinition)
        ApiDefinitionConnectorMock.Prod.FetchPrivate.returns(privateDefinition)
        ApiDefinitionConnectorMock.Sandbox.FetchPublic.returns()
        ApiDefinitionConnectorMock.Sandbox.FetchPrivate.returns()

        val allDefinitions: Future[Seq[ApiDefinitionGK]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Return a filtered API from both environments" in new Setup {

        val expectedApiDefinitions = Seq(customsDeclarations1)

        ApiDefinitionConnectorMock.Prod.FetchPublic.returns(customsDeclarations1)
        ApiDefinitionConnectorMock.Prod.FetchPrivate.returns(customsDeclarations2)
        ApiDefinitionConnectorMock.Sandbox.FetchPublic.returns(customsDeclarations1)
        ApiDefinitionConnectorMock.Sandbox.FetchPrivate.returns(customsDeclarations2)

        val allDefinitions: Future[Seq[ApiDefinitionGK]] = definitionService.fetchAllDistinctApisIgnoreVersions(None)

        await(allDefinitions) shouldBe expectedApiDefinitions
      }

      "Return a combination of public and private APIs in sandbox" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApiDefinitionConnectorMock.Sandbox.FetchPublic.returns(publicDefinition)
        ApiDefinitionConnectorMock.Sandbox.FetchPrivate.returns(privateDefinition)

        val allDefinitions: Future[Seq[ApiDefinitionGK]] = definitionService.fetchAllApiDefinitions(Some(SANDBOX))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector, never).fetchPublic()
        verify(mockProductionApiDefinitionConnector, never).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector).fetchPublic()
        verify(mockSandboxApiDefinitionConnector).fetchPrivate()
      }

      "Return a combination of public and private APIs in production" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApiDefinitionConnectorMock.Prod.FetchPublic.returns(publicDefinition)
        ApiDefinitionConnectorMock.Prod.FetchPrivate.returns(privateDefinition)

        val allDefinitions: Future[Seq[ApiDefinitionGK]] = definitionService.fetchAllApiDefinitions(Some(PRODUCTION))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector).fetchPublic()
        verify(mockProductionApiDefinitionConnector).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector, never).fetchPublic()
        verify(mockSandboxApiDefinitionConnector, never).fetchPrivate()
      }

      "Include no duplicates" in new Setup {

        ApiDefinitionConnectorMock.Prod.FetchPublic.returns(publicDefinition)
        ApiDefinitionConnectorMock.Prod.FetchPrivate.returns(privateDefinition)
        ApiDefinitionConnectorMock.Sandbox.FetchPublic.returns(publicDefinition)
        ApiDefinitionConnectorMock.Sandbox.FetchPrivate.returns(privateDefinition)

        val allDefinitions: Future[Seq[ApiDefinitionGK]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) should have size 2
      }
    }
  }

  "apis" when {
    "get all apis" in new Setup {

      val publicSandbox  = publicDefinition.copy(name = "sandbox-public")
      val privateSandbox = privateDefinition.copy(name = "sandbox-private")

      ApiDefinitionConnectorMock.Prod.FetchPublic.returns(publicDefinition)
      ApiDefinitionConnectorMock.Prod.FetchPrivate.returns(privateDefinition)
      ApiDefinitionConnectorMock.Sandbox.FetchPublic.returns(publicSandbox)
      ApiDefinitionConnectorMock.Sandbox.FetchPrivate.returns(privateSandbox)

      val allDefinitions: Seq[(ApiDefinitionGK, Environment)] = await(definitionService.apis)

      allDefinitions shouldBe Seq(
        (privateDefinition, Environment.PRODUCTION),
        (publicDefinition, Environment.PRODUCTION),
        (privateSandbox, Environment.SANDBOX),
        (publicSandbox, Environment.SANDBOX)
      )
    }
  }

  "apiCategories" when {
    "get all apiCategories" in new Setup {
      val prodCategories    = List(ApiCategoryDetails("Business", "Business"), ApiCategoryDetails("VAT", "Vat"), ApiCategoryDetails("EXAMPLE", "Example"))
      val sandboxCategories = List(ApiCategoryDetails("VAT", "Vat"), ApiCategoryDetails("EXAMPLE", "Example"), ApiCategoryDetails("AGENTS", "Agents"))
      val allCategories     = (prodCategories ++ sandboxCategories).distinct

      ApiDefinitionConnectorMock.Prod.FetchAPICategories.returns(prodCategories: _*)
      ApiDefinitionConnectorMock.Sandbox.FetchAPICategories.returns(sandboxCategories: _*)

      val response: List[ApiCategoryDetails] = await(definitionService.apiCategories())
      response should contain only (allCategories: _*)
    }
  }
}
