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

package connectors

import javax.inject.{Inject, Singleton}
import model.ApiContext
import uk.gov.hmrc.http.HeaderCarrier

import model.applications._
import scala.concurrent.Future

import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.ExecutionContext
import model.subscriptions.ApiData
import model.ApplicationId
import model.Environment.Environment
import model.ApiVersion
import model.FieldName
import model.SubscriptionFields.SubscriptionFieldDefinition
import model.ApiDefinitions
import model.ApiIdentifier
import model.ApplicationUpdateResult
import uk.gov.hmrc.http.HttpResponse
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import model.ApplicationUpdateSuccessResult
import play.api.Logger

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config)(implicit ec: ExecutionContext) {
  import ApmConnectorJsonFormatters._
  import ApmConnector._

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] =
    http.GET[Option[ApplicationWithSubscriptionData]](s"${config.serviceBaseUrl}/applications/${applicationId.value}")

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias] = {
    http.GET[Map[ApiContext, Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]]]](s"${config.serviceBaseUrl}/subscription-fields?environment=$environment")
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Map[ApiContext, ApiData]] = {
    http.GET[Map[ApiContext, ApiData]](
      s"${config.serviceBaseUrl}/api-definitions", 
      Seq(
        applicationIdQueryParam -> applicationId.value,
        restrictedQueryParam -> "false"
      )
    )
  }

  def subscribeToApi(applicationId: ApplicationId, apiIdentifier: ApiIdentifier)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST[ApiIdentifier, HttpResponse](
      s"${config.serviceBaseUrl}/applications/${applicationId.value}/subscriptions?restricted=false",
      apiIdentifier,
      Seq( CONTENT_TYPE -> JSON )
    )
    .map { _ =>
      ApplicationUpdateSuccessResult
    }
  }
}

object ApmConnector {
  val applicationIdQueryParam = "applicationId"
  val restrictedQueryParam = "restricted"

  case class Config(
      serviceBaseUrl: String
  )
}
