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

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.SubscriptionsBuilder
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

class SubscriptionFieldsServiceSpec extends AsyncHmrcSpec {

  trait Setup extends SubscriptionsBuilder {
    val mockSandboxSubscriptionFieldsConnector: SandboxSubscriptionFieldsConnector       = mock[SandboxSubscriptionFieldsConnector]
    val mockProductionSubscriptionFieldsConnector: ProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service                              = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    val underTest: SubscriptionFieldsService = spy(service)
  }

  val apiVersionNbr         = ApiVersionNbr.random
  private val apiIdentifier = ApiIdentifier(ApiContext.random, apiVersionNbr)

  "When application is deployedTo production then principal connector is called" should {
    val application    = mock[ApplicationResponse]
    val newApplication = mock[ApplicationResponse]

    when(application.clientId).thenReturn(ClientId("client-id"))
    when(application.deployedTo).thenReturn(Environment.PRODUCTION)

    when(newApplication.clientId).thenReturn(ClientId("client-id"))
    when(newApplication.deployedTo).thenReturn(Environment.PRODUCTION)

    "saveFieldValues" in new Setup {
      when(mockProductionSubscriptionFieldsConnector.saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(newApplication, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockProductionSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(newApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)

      verify(mockSandboxSubscriptionFieldsConnector, never).saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*)
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {
    val application    = mock[ApplicationResponse]
    val newApplication = mock[ApplicationResponse]

    when(application.clientId).thenReturn(ClientId("client-id"))
    when(application.deployedTo).thenReturn(Environment.SANDBOX)

    when(newApplication.clientId).thenReturn(ClientId("client-id"))
    when(newApplication.deployedTo).thenReturn(Environment.SANDBOX)

    "saveFieldValues" in new Setup {
      when(mockSandboxSubscriptionFieldsConnector.saveFieldValues(*[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(newApplication, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockSandboxSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(newApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)

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
