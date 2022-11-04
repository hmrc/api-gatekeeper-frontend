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
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.connectors.ApmConnector
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications.{ApplicationWithSubscriptionData, NewApplication}
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box
import uk.gov.hmrc.gatekeeper.models.subscriptions._

class ApmService @Inject() (apmConnector: ApmConnector)(implicit ec: ExecutionContext) {

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    apmConnector.fetchApplicationById(applicationId)
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Map[ApiContext, ApiData]] = {
    apmConnector.fetchAllPossibleSubscriptions(applicationId)
  }

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias] = {
    apmConnector.getAllFieldDefinitions(environment)
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    apmConnector.fetchAllCombinedApis()
  }

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    apmConnector.fetchAllBoxes()
  }

  def subscribeToApi(applicationId: ApplicationId, subscribeToApi: SubscribeToApi)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    apmConnector.subscribeToApi(applicationId, subscribeToApi)
  }

  def unsubscribeFromApi(applicationId: ApplicationId, unsubscribeFromApi: UnsubscribeFromApi)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    apmConnector.updateApplication(applicationId, unsubscribeFromApi).map { _: NewApplication => ApplicationUpdateSuccessResult }
  }
}
