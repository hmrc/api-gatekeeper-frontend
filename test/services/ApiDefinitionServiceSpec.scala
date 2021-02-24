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

package services

import connectors._
import model.Environment._
import model._
import org.mockito.BDDMockito._
import uk.gov.hmrc.http.HeaderCarrier
import utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiDefinitionServiceSpec extends AsyncHmrcSpec {
  trait Setup {
    implicit val hc: HeaderCarrier = new HeaderCarrier
    val mockSandboxApiDefinitionConnector = mock[SandboxApiDefinitionConnector]
    val mockProductionApiDefinitionConnector = mock[ProductionApiDefinitionConnector]

    given(mockSandboxApiDefinitionConnector.environment).willReturn(Environment.SANDBOX)
    given(mockProductionApiDefinitionConnector.environment).willReturn(Environment.PRODUCTION)

    val definitionService = new ApiDefinitionService(mockSandboxApiDefinitionConnector, mockProductionApiDefinitionConnector)

    val publicDefinition = ApiDefinition(
      "publicAPI", "http://localhost/",
      "publicAPI", "public api.", ApiContext.random,
      List(ApiVersionDefinition(ApiVersion.random, ApiStatus.STABLE, Some(ApiAccess(APIAccessType.PUBLIC)))), Some(false), None
    )

    val privateDefinition = ApiDefinition(
      "privateAPI", "http://localhost/",
      "privateAPI", "private api.", ApiContext.random,
      List(ApiVersionDefinition(ApiVersion.random, ApiStatus.STABLE, Some(ApiAccess(APIAccessType.PRIVATE)))), Some(false), None
    )


    val version1 = ApiVersionDefinition(ApiVersion("1.0"), ApiStatus.BETA, Some(ApiAccess(APIAccessType.PUBLIC)))
    val version2 = ApiVersionDefinition(ApiVersion("2.0"), ApiStatus.BETA, Some(ApiAccess(APIAccessType.PRIVATE)))
    val version3 = ApiVersionDefinition(ApiVersion("3.0"), ApiStatus.BETA, Some(ApiAccess(APIAccessType.PRIVATE)))

    val customsDeclarations1 = ApiDefinition(serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = List(version1),
      categories = Some(List(APICategory("CUSTOMS")))
    )

    val customsDeclarations2 = ApiDefinition(serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = List(version2.copy(), version3.copy()),
      categories = Some(List(APICategory("CUSTOMS")))
    )
  }

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs in both environments" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(List.empty))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(List.empty))

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Return a filtered API from both environments" in new Setup {

        val expectedApiDefinitions = Seq(customsDeclarations1)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(List(customsDeclarations1)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(customsDeclarations2)))
        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(List(customsDeclarations1)))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(customsDeclarations2)))

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllDistinctApisIgnoreVersions(None)

        await(allDefinitions) shouldBe expectedApiDefinitions
      }

      "Return a combination of public and private APIs in sandbox" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition)))

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(SANDBOX))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector, never).fetchPublic()
        verify(mockProductionApiDefinitionConnector, never).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector).fetchPublic()
        verify(mockSandboxApiDefinitionConnector).fetchPrivate()
      }

      "Return a combination of public and private APIs in production" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition)))

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(PRODUCTION))

        await(allDefinitions) shouldBe expectedApiDefintions

        verify(mockProductionApiDefinitionConnector).fetchPublic()
        verify(mockProductionApiDefinitionConnector).fetchPrivate()
        verify(mockSandboxApiDefinitionConnector, never).fetchPublic()
        verify(mockSandboxApiDefinitionConnector, never).fetchPrivate()
      }

      "Include no duplicates" in new Setup {

        given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition, publicDefinition)))
        given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition, privateDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition, publicDefinition)))
        given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition, privateDefinition)))

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) should have size 2
      }
    }
  }

  "apis" when {
    "get all apis" in new Setup {

      val publicSandbox = publicDefinition.copy(name="sandbox-public")
      val privateSandbox = privateDefinition.copy(name="sandbox-private")

      given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicDefinition)))
      given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateDefinition)))
      given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(List(publicSandbox)))
      given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(List(privateSandbox)))

      val allDefinitions: Seq[(ApiDefinition, Environment)] = await(definitionService.apis)

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
      val prodCategories = List(APICategoryDetails("Business", "Business"), APICategoryDetails("VAT", "Vat"), APICategoryDetails("EXAMPLE", "Example"))
      val sandboxCategories = List(APICategoryDetails("VAT", "Vat"), APICategoryDetails("EXAMPLE", "Example"), APICategoryDetails("AGENTS", "Agents"))
      val allCategories = (prodCategories ++ sandboxCategories).distinct
      
      given(mockProductionApiDefinitionConnector.fetchAPICategories()).willReturn(Future(prodCategories))
      given(mockSandboxApiDefinitionConnector.fetchAPICategories()).willReturn(Future(sandboxCategories))

      val response: List[APICategoryDetails] = await(definitionService.apiCategories)
      response should contain only (allCategories:_*)
    }
  }
}
