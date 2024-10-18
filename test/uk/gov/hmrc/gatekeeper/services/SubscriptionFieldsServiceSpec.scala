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

import scala.concurrent.Future.successful

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaboratorsFixtures
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Environment, _}
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.SubscriptionsBuilder
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

class SubscriptionFieldsServiceSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures with ApiIdentifierFixtures {

  private val apiIdentifier = apiIdentifierOne

  val productionApplication = standardApp.withEnvironment(Environment.PRODUCTION) // , clientId = clientIdTwo))
  val sandboxApplication    = standardApp.inSandbox()                             // , clientId = clientIdTwo))

  trait Setup extends SubscriptionsBuilder {
    val mockSandboxSubscriptionFieldsConnector: SandboxSubscriptionFieldsConnector       = mock[SandboxSubscriptionFieldsConnector]
    val mockProductionSubscriptionFieldsConnector: ProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    // val underTest: SubscriptionFieldsService = spy(service)
  }

  "When application is deployedTo production then principal connector is called" should {

    "saveFieldValues" in new Setup {
      when(mockProductionSubscriptionFieldsConnector.saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(productionApplication.details, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockProductionSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(productionApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)

      verify(mockSandboxSubscriptionFieldsConnector, never).saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*)
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {

    "saveFieldValues" in new Setup {
      when(mockSandboxSubscriptionFieldsConnector.saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(sandboxApplication.details, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockSandboxSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(sandboxApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)

      verify(mockProductionSubscriptionFieldsConnector, never).saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*)
    }
  }

  "fetchAllProductionFieldValues" in new Setup {
    val expectedResult = List.empty

    when(mockProductionSubscriptionFieldsConnector.fetchAllFieldValues()(*))
      .thenReturn(successful(expectedResult))

    val actualResult = await(service.fetchAllProductionFieldValues()(*))

    actualResult shouldBe expectedResult

    verify(mockProductionSubscriptionFieldsConnector)
      .fetchAllFieldValues()(*)
  }
}
