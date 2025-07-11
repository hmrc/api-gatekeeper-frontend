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

import javax.inject.Inject
import scala.concurrent.Future

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithSubscriptionFields, CoreApplication}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, _}
import uk.gov.hmrc.gatekeeper.connectors.ApmConnector
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SaveSubscriptionFieldsResponse}
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box

class ApmService @Inject() (apmConnector: ApmConnector) {

  println("FROM APMSERVICE " + apmConnector)

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFields]] = {
    apmConnector.fetchApplicationById(applicationId)
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    apmConnector.fetchAllPossibleSubscriptions(applicationId)
  }

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitionFields.Alias] = {
    apmConnector.getAllFieldDefinitions(environment)
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    apmConnector.fetchAllCombinedApis()
  }

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    apmConnector.fetchAllBoxes()
  }

  def fetchNonOpenApis(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    apmConnector.fetchNonOpenApis(environment)
  }

  def subsFieldsCsv()(implicit hc: HeaderCarrier): Future[String] = {
    apmConnector.subsFieldsCsv(Environment.PRODUCTION)
  }

  def saveFieldValues(
      application: CoreApplication,
      apiContext: ApiContext,
      apiVersion: ApiVersionNbr,
      fields: Fields.Alias
    )(implicit hc: HeaderCarrier
    ): Future[SaveSubscriptionFieldsResponse] = {
    apmConnector.saveFieldValues(application.deployedTo, application.clientId, apiContext, apiVersion, fields)
  }
}
