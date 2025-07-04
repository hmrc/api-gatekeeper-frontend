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

import javax.inject.{Inject, Named, Singleton}
import scala.concurrent.Future

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.CoreApplication
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.services.SubscriptionFieldsService._

@Singleton
class SubscriptionFieldsService @Inject() (
    @Named("SANDBOX") sandboxSubscriptionFieldsConnector: SubscriptionFieldsConnector,
    @Named("PRODUCTION") productionSubscriptionFieldsConnector: SubscriptionFieldsConnector
  ) extends APIDefinitionFormatters {

  def saveFieldValues(
      application: CoreApplication,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Fields.Alias
    )(implicit hc: HeaderCarrier
    ): Future[SaveSubscriptionFieldsResponse] = {
    connectorFor(application.deployedTo).saveFieldValues(application.clientId, apiContext, apiVersion, fields)
  }

  private def connectorFor(deployedTo: Environment): SubscriptionFieldsConnector =
    if (deployedTo == Environment.PRODUCTION) {
      productionSubscriptionFieldsConnector
    } else {
      sandboxSubscriptionFieldsConnector
    }
}

object SubscriptionFieldsService {

  trait SubscriptionFieldsConnector {

    def saveFieldValues(
        clientId: ClientId,
        apiContext: ApiContext,
        apiVersion: ApiVersionNbr,
        fields: Fields.Alias
      )(implicit hc: HeaderCarrier
      ): Future[SaveSubscriptionFieldsResponse]
  }
}
