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
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.SubscriptionsBuilder
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._

class SubscriptionFieldsServiceSpec extends AsyncHmrcSpec with ApplicationWithCollaboratorsFixtures with ApiIdentifierFixtures {

  private val apiIdentifier = apiIdentifierOne

  val productionApplication = standardApp
  val sandboxApplication    = standardApp.inSandbox()

  trait Setup extends SubscriptionsBuilder {
    val mockApmConnectorModule: ApmConnectorSubscriptionFieldsModule = mock[ApmConnectorSubscriptionFieldsModule]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockApmConnectorModule)
  }

  "When application is deployedTo production then principal connector is called" should {

    "saveFieldValues" in new Setup {
      when(mockApmConnectorModule.saveFieldValues(*[Environment], *[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(productionApplication.details, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockApmConnectorModule).saveFieldValues(eqTo(Environment.PRODUCTION), eqTo(productionApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)
      verify(mockApmConnectorModule, never).saveFieldValues(eqTo(Environment.SANDBOX), *[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*)
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {

    "saveFieldValues" in new Setup {
      when(mockApmConnectorModule.saveFieldValues(*[Environment], *[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields.Alias = mock[Fields.Alias]

      await(service.saveFieldValues(sandboxApplication.details, apiIdentifier.context, apiIdentifier.versionNbr, fields))

      verify(mockApmConnectorModule)
        .saveFieldValues(eqTo(Environment.SANDBOX), eqTo(productionApplication.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.versionNbr), eqTo(fields))(*)
      verify(mockApmConnectorModule, never) 
        .saveFieldValues(eqTo(Environment.PRODUCTION), *[ClientId], *[ApiContext], *[ApiVersionNbr], *)(*)
    }
  }
}
