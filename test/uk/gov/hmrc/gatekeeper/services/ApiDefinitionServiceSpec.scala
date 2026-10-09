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

import mocks.connectors.ApmConnectorMockProvider
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

class ApiDefinitionServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApmConnectorMockProvider {
    implicit val hc: HeaderCarrier = new HeaderCarrier

    val definitionService = new ApiDefinitionService(ApmConnectorMock.aMock)

    val apiVersion1 = ApiVersionNbr.random

    val publicDefinition = ApiDefinition(
      ServiceName("publicAPI"),
      ApiDefinition.ServiceBaseUrl("http://localhost/"),
      ApiDefinition.Name("publicAPI"),
      ApiDefinition.Description("public api."),
      ApiContext.random,
      Map(apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.Stable, ApiAccessType.Public, List.empty, false, None, ApiVersionSource.Unknown)),
      false,
      None,
      List(ApiCategory.Other)
    )

    val privateDefinition = ApiDefinition(
      ServiceName("privateAPI"),
      ApiDefinition.ServiceBaseUrl("http://localhost/"),
      ApiDefinition.Name("privateAPI"),
      ApiDefinition.Description("private api."),
      ApiContext.random,
      Map(apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.Stable, ApiAccessType.Internal, List.empty, false, None, ApiVersionSource.Unknown)),
      false,
      None,
      List(ApiCategory.Other)
    )

    val version1 = ApiVersion(ApiVersionNbr("1.0"), ApiStatus.Beta, ApiAccessType.Public, List.empty, false, None, ApiVersionSource.Unknown)
    val version2 = ApiVersion(ApiVersionNbr("2.0"), ApiStatus.Beta, ApiAccessType.Controlled, List.empty, false, None, ApiVersionSource.Unknown)
    val version3 = ApiVersion(ApiVersionNbr("3.0"), ApiStatus.Beta, ApiAccessType.Internal, List.empty, false, None, ApiVersionSource.Unknown)

    val customsDeclarations1 = ApiDefinition(
      serviceName = ServiceName("customs-declarations"),
      serviceBaseUrl = ApiDefinition.ServiceBaseUrl("https://customs-declarations.protected.mdtp"),
      name = ApiDefinition.Name("Customs Declarations"),
      description = ApiDefinition.Description("Single WCO-compliant Customs Declarations API"),
      context = ApiContext("customs/declarations"),
      versions = Map(version1.versionNbr -> version1),
      categories = List(ApiCategory.Customs),
      isTestSupport = false,
      lastPublishedAt = None
    )

    val customsDeclarations2 = ApiDefinition(
      serviceName = ServiceName("customs-declarations"),
      serviceBaseUrl = ApiDefinition.ServiceBaseUrl("https://customs-declarations.protected.mdtp"),
      name = ApiDefinition.Name("Customs Declarations"),
      description = ApiDefinition.Description("Single WCO-compliant Customs Declarations API"),
      context = ApiContext("customs/declarations"),
      versions = Map(version2.versionNbr -> version2.copy(), version3.versionNbr -> version3.copy()),
      categories = List(ApiCategory.Customs),
      isTestSupport = false,
      lastPublishedAt = None
    )
  }

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs in both environments" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Production)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Sandbox)()

        val allDefinitions: Future[List[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Return a combination of public and private APIs in sandbox" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Production)()
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Sandbox)(publicDefinition, privateDefinition)

        val allDefinitions: Future[List[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(Environment.Sandbox))

        await(allDefinitions) shouldBe expectedApiDefintions

        ApmConnectorMock.FetchAllApiDefinitions.verifyNeverCalledFor(Environment.Production)
        ApmConnectorMock.FetchAllApiDefinitions.verifyCalledFor(Environment.Sandbox)
      }

      "Return a combination of public and private APIs in production" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Production)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Sandbox)()

        val allDefinitions: Future[List[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(Environment.Production))

        await(allDefinitions) shouldBe expectedApiDefintions

        ApmConnectorMock.FetchAllApiDefinitions.verifyCalledFor(Environment.Production)
        ApmConnectorMock.FetchAllApiDefinitions.verifyNeverCalledFor(Environment.Sandbox)
      }

      "Include no duplicates" in new Setup {

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Production)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Sandbox)(publicDefinition, privateDefinition)

        val allDefinitions: Future[List[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) should have size 2
      }
    }
  }

  "apis" when {
    "get all apis" in new Setup {

      val publicSandbox  = publicDefinition.copy(name = ApiDefinition.Name("sandbox-public"))
      val privateSandbox = privateDefinition.copy(name = ApiDefinition.Name("sandbox-private"))

      ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Production)(publicDefinition, privateDefinition)
      ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.Sandbox)(publicSandbox, privateSandbox)

      val allDefinitions: List[(ApiDefinition, Environment)] = await(definitionService.apis)

      allDefinitions shouldBe Seq(
        (privateDefinition, Environment.Production),
        (publicDefinition, Environment.Production),
        (privateSandbox, Environment.Sandbox),
        (publicSandbox, Environment.Sandbox)
      )
    }
  }
}
