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

import javax.inject.{Inject, Named, Singleton}
import model.SubscriptionFields.{Fields, SubscriptionFieldDefinition, SubscriptionFieldValue}
import model.{ApiContextVersion, Application, FieldsDeleteResult}
import services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SubscriptionFieldsService @Inject()(@Named("SANDBOX") sandboxSubscriptionFieldsConnector: SubscriptionFieldsConnector,
                                          @Named("PRODUCTION")productionSubscriptionFieldsConnector: SubscriptionFieldsConnector)(implicit ec: ExecutionContext) {

  def fetchAllFieldDefinitions(deployedTo: String)(implicit hc: HeaderCarrier) : Future[DefinitionsByApiVersion] = {
     connectorFor(deployedTo).fetchAllFieldDefinitions()
  }

  def fetchFieldsWithPrefetchedDefinitions(application: Application,
                                           apiContextVersion: ApiContextVersion,
                                           definitions: DefinitionsByApiVersion)
                                          (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    connectorFor(application).fetchFieldsValuesWithPrefetchedDefinitions(application.clientId, apiContextVersion, definitions)
  }

  def saveFieldValues(application: Application, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    connectorFor(application).saveFieldValues(application.clientId, apiContext, apiVersion, fields)
  }

  def deleteFieldValues(application: Application, context: String, version: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    connectorFor(application).deleteFieldValues(application.clientId, context, version)
  }

  def connectorFor(application: Application): SubscriptionFieldsConnector = connectorFor(application.deployedTo)

  def connectorFor(deployedTo: String): SubscriptionFieldsConnector =
    if (deployedTo == "PRODUCTION") {
      productionSubscriptionFieldsConnector
    } else {
      sandboxSubscriptionFieldsConnector
    }
}

object SubscriptionFieldsService {
  trait SubscriptionFieldsConnector {
    def fetchFieldsValuesWithPrefetchedDefinitions(clientId: String, apiContextVersion: ApiContextVersion, definitionsCache: DefinitionsByApiVersion)
                                                  (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]]

    def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion]

    def saveFieldValues(clientId: String, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse]

    def deleteFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult]
  }

  type DefinitionsByApiVersion = Map[ApiContextVersion, Seq[SubscriptionFieldDefinition]]

  object DefinitionsByApiVersion {
    val empty = Map.empty[ApiContextVersion, Seq[SubscriptionFieldDefinition]]
  }
}
