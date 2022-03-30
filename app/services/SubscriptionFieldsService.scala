/*
 * Copyright 2022 HM Revenue & Customs
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

import javax.inject.{Inject, Named, Singleton}
import model.SubscriptionFields._
import model._
import services.SubscriptionFieldsService._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import model.applications.NewApplication

@Singleton
class SubscriptionFieldsService @Inject()(@Named("SANDBOX") sandboxSubscriptionFieldsConnector: SubscriptionFieldsConnector,
                                          @Named("PRODUCTION")productionSubscriptionFieldsConnector: SubscriptionFieldsConnector)
                                          extends APIDefinitionFormatters {

  def saveFieldValues(application: NewApplication, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)
      (implicit hc: HeaderCarrier): Future[SaveSubscriptionFieldsResponse] = {
    connectorFor(application.deployedTo.toString).saveFieldValues(application.clientId, apiContext, apiVersion, fields)
  }

  private def connectorFor(deployedTo: String): SubscriptionFieldsConnector =
    if (deployedTo == "PRODUCTION") {
      productionSubscriptionFieldsConnector
    } else {
      sandboxSubscriptionFieldsConnector
    }
}

object SubscriptionFieldsService {
  trait SubscriptionFieldsConnector {
    def saveFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)(implicit hc: HeaderCarrier): Future[SaveSubscriptionFieldsResponse]
  }

  type DefinitionsByApiVersion = Map[ApiIdentifier, List[SubscriptionFieldDefinition]]

  object DefinitionsByApiVersion {
    val empty = Map.empty[ApiIdentifier, List[SubscriptionFieldDefinition]]
  }
}
