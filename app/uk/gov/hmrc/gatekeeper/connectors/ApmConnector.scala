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

package uk.gov.hmrc.gatekeeper.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition
import uk.gov.hmrc.gatekeeper.models.applications._
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData
import uk.gov.hmrc.gatekeeper.models.{CombinedApi, _}

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config)(implicit ec: ExecutionContext) {
  import ApmConnectorJsonFormatters._
  import ApmConnector._

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] =
    http.GET[Option[ApplicationWithSubscriptionData]](s"${config.serviceBaseUrl}/applications/${applicationId.value.toString}")

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias] = {
    http.GET[Map[ApiContext, Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]]]](s"${config.serviceBaseUrl}/subscription-fields?environment=$environment")
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Map[ApiContext, ApiData]] = {
    http.GET[Map[ApiContext, ApiData]](
      url = s"${config.serviceBaseUrl}/api-definitions",
      queryParams = Seq(
        applicationIdQueryParam -> applicationId.value.toString(),
        restrictedQueryParam    -> "false"
      ),
      headers = Seq.empty[(String, String)]
    )
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.GET[List[CombinedApi]](s"${config.serviceBaseUrl}/combined-rest-xml-apis")
  }

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    http.GET[List[Box]](s"${config.serviceBaseUrl}/push-pull-notifications/boxes")
  }

  // TODO - better return type
  // TODO - better error handling for expected errors
  def update(applicationId: ApplicationId, cmd: ApplicationCommand)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    val url = s"${config.serviceBaseUrl}/applications/${applicationId.value.toString()}"
    http.PATCH[ApplicationCommand, Either[UpstreamErrorResponse, Unit]](url, cmd)
  }
}

object ApmConnector {
  val applicationIdQueryParam = "applicationId"
  val restrictedQueryParam    = "restricted"

  case class Config(
      serviceBaseUrl: String
    )
}
