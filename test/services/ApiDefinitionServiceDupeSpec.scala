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
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApiDefinitionServiceDupeSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {
    implicit val hc: HeaderCarrier = new HeaderCarrier
    val mockSandboxApiDefinitionConnector = mock[SandboxApiDefinitionConnector]
    val mockProductionApiDefinitionConnector = mock[ProductionApiDefinitionConnector]

    given(mockSandboxApiDefinitionConnector.environment).willReturn(Environment.SANDBOX)
    given(mockProductionApiDefinitionConnector.environment).willReturn(Environment.PRODUCTION)

    val definitionService = new ApiDefinitionService(mockSandboxApiDefinitionConnector, mockProductionApiDefinitionConnector)

    val version1 = ApiVersionDefinition(ApiVersion("1.0"), APIStatus.BETA, Some(APIAccess(APIAccessType.PUBLIC)))
    val version2 = ApiVersionDefinition(ApiVersion("2.0"), APIStatus.BETA, Some(APIAccess(APIAccessType.PRIVATE)))
    val version3 = ApiVersionDefinition(ApiVersion("3.0"), APIStatus.BETA, Some(APIAccess(APIAccessType.PRIVATE)))

    val customsDeclarations1 = APIDefinition(serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = Seq(version1),
      categories = Some(Seq(APICategory("CUSTOMS")))
    )

    val customsDeclarations2 = APIDefinition(serviceName = "customs-declarations",
      serviceBaseUrl = "https://customs-declarations.protected.mdtp",
      name = "Customs Declarations",
      description = "Single WCO-compliant Customs Declarations API",
      context = ApiContext("customs/declarations"),
      requiresTrust = Some(false),
      versions = Seq(version2.copy(), version3.copy()),
      categories = Some(Seq(APICategory("CUSTOMS")))
    )
  }

    "DefinitionService" when {

      "Definitions are requested" should {

        "Return a combination of public and private APIs in both environments" in new Setup {

          val expectedApiDefintions = Seq(customsDeclarations1)

          given(mockProductionApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(customsDeclarations1)))
          given(mockProductionApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(customsDeclarations2)))
          given(mockSandboxApiDefinitionConnector.fetchPublic()).willReturn(Future(Seq(customsDeclarations1.copy())))
          given(mockSandboxApiDefinitionConnector.fetchPrivate()).willReturn(Future(Seq(customsDeclarations2.copy())))

          val allDefinitions: Future[Seq[APIDefinition]] = definitionService.fetchAllDistinctApisIgnoreVersions(None)
          allDefinitions.size shouldBe 1
          await(allDefinitions) shouldBe expectedApiDefintions
        }
      }

    }



}
