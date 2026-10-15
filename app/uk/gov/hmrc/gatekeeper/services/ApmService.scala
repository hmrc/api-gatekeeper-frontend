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

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, CombinedApi}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.CoreApplication
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, *}
import uk.gov.hmrc.gatekeeper.connectors.ApmConnector
import uk.gov.hmrc.gatekeeper.models.*
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SaveSubscriptionFieldsResponse}
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box

@Singleton
class ApmService @Inject() (apmConnector: ApmConnector) {

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(using hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    apmConnector.fetchAllPossibleSubscriptions(applicationId)
  }

  def getAllFieldDefinitions(environment: Environment)(using hc: HeaderCarrier): Future[ApiDefinitionFields.Alias] = {
    apmConnector.getAllFieldDefinitions(environment)
  }

  def fetchAllCombinedApis()(using hc: HeaderCarrier): Future[List[CombinedApi]] = {
    apmConnector.fetchAllCombinedApis()
  }

  def fetchAllBoxes()(using hc: HeaderCarrier): Future[List[Box]] = {
    apmConnector.fetchAllBoxes()
  }

  def fetchNonOpenApis(environment: Environment)(using hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    apmConnector.fetchNonOpenApis(environment)
  }

  def subsFieldsCsv()(using hc: HeaderCarrier): Future[String] = {
    apmConnector.subsFieldsCsv(Environment.Production)
  }

  def saveFieldValues(
      application: CoreApplication,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Fields.Alias
    )(using hc: HeaderCarrier
    ): Future[SaveSubscriptionFieldsResponse] = {
    apmConnector.saveFieldValues(application.deployedTo, application.token.clientId, apiContext, apiVersion, fields)
  }
}
