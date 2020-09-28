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
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsResponse, SaveSubscriptionFieldsSuccessResponse, SubscriptionFieldDefinition, SubscriptionFieldValue}
import model.{APIDefinitionFormatters, ApiIdentifier, ApiContext, ApiVersion, Application, ClientId, FieldName, FieldValue, FieldsDeleteResult}
import services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

@Singleton
class SubscriptionFieldsService @Inject()(@Named("SANDBOX") sandboxSubscriptionFieldsConnector: SubscriptionFieldsConnector,
                                          @Named("PRODUCTION")productionSubscriptionFieldsConnector: SubscriptionFieldsConnector)
                                          extends APIDefinitionFormatters {

  def fetchFieldsValues(application: Application, fieldDefinitions: Seq[SubscriptionFieldDefinition], apiIdentifier: ApiIdentifier)
                       (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    val connector = connectorFor(application)

    if (fieldDefinitions.isEmpty) {
      Future.successful(Seq.empty[SubscriptionFieldValue])
    } else {
      connector.fetchFieldValues(application.clientId, apiIdentifier.context, apiIdentifier.version)
    }
  }

  def fetchAllFieldDefinitions(deployedTo: String)(implicit hc: HeaderCarrier) : Future[DefinitionsByApiVersion] = {
     connectorFor(deployedTo).fetchAllFieldDefinitions()
  }

  def fetchFieldDefinitions(deployedTo: String, apiIdentifier: ApiIdentifier)
                           (implicit hc: HeaderCarrier) : Future[Seq[SubscriptionFieldDefinition]] = {
    connectorFor(deployedTo)
      .fetchFieldDefinitions(apiIdentifier.context, apiIdentifier.version)
  }

  def fetchFieldsWithPrefetchedDefinitions(application: Application,
                                           apiIdentifier: ApiIdentifier,
                                           definitions: DefinitionsByApiVersion)
                                          (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    connectorFor(application).fetchFieldsValuesWithPrefetchedDefinitions(application.clientId, apiIdentifier, definitions)
  }

  def saveFieldValues(application: Application, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)
      (implicit hc: HeaderCarrier): Future[SaveSubscriptionFieldsResponse] = {
    connectorFor(application).saveFieldValues(application.clientId, apiContext, apiVersion, fields)
  }

  def saveBlankFieldValues( application: Application,
                            apiContext: ApiContext,
                            apiVersion: ApiVersion,
                            values : Seq[SubscriptionFieldValue])
                          (implicit hc: HeaderCarrier) : Future[SaveSubscriptionFieldsResponse] = {

    def createEmptyFieldValues(fieldDefinitions: Seq[SubscriptionFieldDefinition]) = {
      fieldDefinitions
        .map(d => d.name -> FieldValue.empty)
        .toMap[FieldName, FieldValue]
    }

    if(values.forall(_.value.isEmpty)){
      val connector = connectorFor(application)

      val emptyFieldValues = createEmptyFieldValues(values.map(_.definition))

      connector.saveFieldValues(application.clientId, apiContext, apiVersion, emptyFieldValues)
    } else {
      Future.successful(SaveSubscriptionFieldsSuccessResponse)
    }
  }

  def deleteFieldValues(application: Application, context: ApiContext, version: ApiVersion)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
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
    def fetchFieldValues(clientId: ClientId, apiContext: ApiContext, version: ApiVersion)
                        (implicit hc: HeaderCarrier) : Future[Seq[SubscriptionFieldValue]]

    def fetchFieldsValuesWithPrefetchedDefinitions(clientId: ClientId, apiIdentifier: ApiIdentifier, definitionsCache: DefinitionsByApiVersion)
                                                  (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]]

    def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion]

    def fetchFieldDefinitions(apiContext: ApiContext, apiVersion: ApiVersion)
                             (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldDefinition]]

    def saveFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)(implicit hc: HeaderCarrier): Future[SaveSubscriptionFieldsResponse]

    def deleteFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult]
  }

  type DefinitionsByApiVersion = Map[ApiIdentifier, Seq[SubscriptionFieldDefinition]]

  object DefinitionsByApiVersion {
    val empty = Map.empty[ApiIdentifier, Seq[SubscriptionFieldDefinition]]
  }
}
