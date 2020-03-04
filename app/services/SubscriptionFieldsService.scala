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

import javax.inject.Inject
import connectors._
import model.apiSubscriptionFields.{Fields, SubscriptionFieldDefinition, SubscriptionFieldValue}
import model.{APIVersion, ApiContextVersion, Application, FieldsDeleteResult, apiSubscriptionFields}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}

import scala.concurrent.{ExecutionContext, Future}

class SubscriptionFieldsService @Inject()(sandboxSubscriptionFieldsConnector: SandboxSubscriptionFieldsConnector,
                                          productionSubscriptionFieldsConnector: ProductionSubscriptionFieldsConnector)(implicit ec: ExecutionContext) {

  // TODO: Rename to fetchFieldValues
  def fetchFields(application: Application, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    val connector = connectorFor(application)

    connector.fetchFieldsValues(application.clientId, ApiContextVersion(apiContext, apiVersion))
  }

  def saveFieldValues(application: Application, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    connectorFor(application).saveFieldValues(application.clientId, apiContext, apiVersion, fields)
  }

  def deleteFieldValues(application: Application, context: String, version: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    connectorFor(application).deleteFieldValues(application.clientId, context, version)
  }

  def connectorFor(application: Application): SubscriptionFieldsConnector =
    if (application.deployedTo == "PRODUCTION") productionSubscriptionFieldsConnector else sandboxSubscriptionFieldsConnector
}
