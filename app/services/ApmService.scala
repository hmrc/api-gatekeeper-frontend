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

import connectors.ApmConnector
import model.ApplicationId
import model.ApiDefinitions
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier
import javax.inject.Inject
import model.applications.ApplicationWithSubscriptionData
import model.subscriptions._
import model.ApiContext
import model.Environment.Environment
import model.CombinedApi

class ApmService @Inject() (apmConnector: ApmConnector) {

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] = {
    apmConnector.fetchApplicationById(applicationId)
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Map[ApiContext, ApiData]] = {
    apmConnector.fetchAllPossibleSubscriptions(applicationId)
  }  
  
  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias]  = {
    apmConnector.getAllFieldDefinitions(environment)
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]]  = {
    apmConnector.fetchAllCombinedApis()
  }
}
