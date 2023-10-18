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

    val publicDefinition = ApiDefinition(
      ServiceName("publicAPI"),
      "http://localhost/",
      "publicAPI",
      "public api.",
      ApiContext.random,
      List(ApiVersion(ApiVersionNbr.random, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)),
      false,
      false,
      None,
      List(ApiCategory.OTHER)
    )

    val privateDefinition = ApiDefinition(
      ServiceName("privateAPI"),
      "http://localhost/",
      "privateAPI",
      "private api.",
      ApiContext.random,
      List(ApiVersion(ApiVersionNbr.random, ApiStatus.STABLE, ApiAccess.Private(false), List.empty, false, None, ApiVersionSource.UNKNOWN)),
      false,
      false,
      None,
      List(ApiCategory.OTHER)
    )

    val version1 = ApiVersion(ApiVersionNbr("1.0"), ApiStatus.BETA, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)
    val version2 = ApiVersion(ApiVersionNbr("2.0"), ApiStatus.BETA, ApiAccess.Private(false), List.empty, false, None, ApiVersionSource.UNKNOWN)
    val version3 = ApiVersion(ApiVersionNbr("3.0"), ApiStatus.BETA, ApiAccess.Private(false), List.empty, false, None, ApiVersionSource.UNKNOWN)

    val customsDeclarations1 = ApiDefinition(
      serviceName = ServiceName("customs-declarations"),
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = false,
      versions = List(version1),
      categories = List(ApiCategory.CUSTOMS)
    )

    val customsDeclarations2 = ApiDefinition(
      serviceName = ServiceName("customs-declarations"),
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = false,
      versions = List(version2.copy(), version3.copy()),
      categories = List(ApiCategory.CUSTOMS)
    )
  }

  "DefinitionService" when {

    "Definitions are requested" should {

      "Return a combination of public and private APIs in both environments" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.PRODUCTION)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.SANDBOX)()

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) shouldBe expectedApiDefintions
      }

      "Return a combination of public and private APIs in sandbox" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.PRODUCTION)()
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.SANDBOX)(publicDefinition, privateDefinition)

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(Environment.SANDBOX))

        await(allDefinitions) shouldBe expectedApiDefintions

        ApmConnectorMock.FetchAllApiDefinitions.verifyNeverCalledFor(Environment.PRODUCTION)
        ApmConnectorMock.FetchAllApiDefinitions.verifyCalledFor(Environment.SANDBOX)
      }

      "Return a combination of public and private APIs in production" in new Setup {

        val expectedApiDefintions = Seq(publicDefinition, privateDefinition)

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.PRODUCTION)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.SANDBOX)()

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(Some(Environment.PRODUCTION))

        await(allDefinitions) shouldBe expectedApiDefintions

        ApmConnectorMock.FetchAllApiDefinitions.verifyCalledFor(Environment.PRODUCTION)
        ApmConnectorMock.FetchAllApiDefinitions.verifyNeverCalledFor(Environment.SANDBOX)
      }

      "Include no duplicates" in new Setup {

        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.PRODUCTION)(publicDefinition, privateDefinition)
        ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.SANDBOX)(publicDefinition, privateDefinition)

        val allDefinitions: Future[Seq[ApiDefinition]] = definitionService.fetchAllApiDefinitions(None)

        await(allDefinitions) should have size 2
      }
    }
  }

  "apis" when {
    "get all apis" in new Setup {

      val publicSandbox  = publicDefinition.copy(name = "sandbox-public")
      val privateSandbox = privateDefinition.copy(name = "sandbox-private")

      ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.PRODUCTION)(publicDefinition, privateDefinition)
      ApmConnectorMock.FetchAllApiDefinitions.returnsFor(Environment.SANDBOX)(publicSandbox, privateSandbox)

      val allDefinitions: Seq[(ApiDefinition, Environment)] = await(definitionService.apis)

      allDefinitions shouldBe Seq(
        (privateDefinition, Environment.PRODUCTION),
        (publicDefinition, Environment.PRODUCTION),
        (privateSandbox, Environment.SANDBOX),
        (publicSandbox, Environment.SANDBOX)
      )
    }
  }
}
