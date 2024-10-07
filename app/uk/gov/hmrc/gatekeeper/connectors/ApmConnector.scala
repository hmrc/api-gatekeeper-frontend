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

import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse, _}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, MappedApiDefinitions}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptionFields
import uk.gov.hmrc.apiplatform.modules.applications.subscriptions.domain.models.FieldName
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box

object ApmConnector {
  val applicationIdQueryParam = "applicationId"
  val restrictedQueryParam    = "restricted"

  case class Config(
      serviceBaseUrl: String
    )
}

@Singleton
class ApmConnector @Inject() (http: HttpClientV2, config: ApmConnector.Config)(implicit ec: ExecutionContext) {
  import ApmConnectorJsonFormatters._
  import ApmConnector._

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionFields]] =
    http.get(url"${config.serviceBaseUrl}/applications/${applicationId}")
      .execute[Option[ApplicationWithSubscriptionFields]]

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitionFields.Alias] = {
    http.get((url"${config.serviceBaseUrl}/subscription-fields?environment=$environment"))
      .execute[Map[ApiContext, Map[ApiVersionNbr, Map[FieldName, SubscriptionFieldDefinition]]]]
  }

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val queryParams = Seq(
      applicationIdQueryParam -> applicationId.value.toString(),
      restrictedQueryParam    -> "false"
    )
    http.get(url"${config.serviceBaseUrl}/api-definitions?$queryParams")
      .execute[MappedApiDefinitions]
      .map(_.wrapped.values.toList)
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.get(url"${config.serviceBaseUrl}/combined-rest-xml-apis")
      .execute[List[CombinedApi]]
  }

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    http.get(url"${config.serviceBaseUrl}/push-pull-notifications/boxes")
      .execute[List[Box]]
  }

  def fetchNonOpenApis(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.get(url"${config.serviceBaseUrl}/api-definitions/nonopen?environment=$environment")
      .execute[List[ApiDefinition]]
  }

  def fetchAllApis(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.get(url"${config.serviceBaseUrl}/api-definitions/all?environment=$environment")
      .execute[MappedApiDefinitions]
      .map(_.wrapped.values.toList)
  }

  // TODO - better return type
  // TODO - better error handling for expected errors
  def update(applicationId: ApplicationId, cmd: ApplicationCommand)(implicit hc: HeaderCarrier): Future[Either[UpstreamErrorResponse, Unit]] = {
    http.patch(url"${config.serviceBaseUrl}/applications/${applicationId.value.toString()}")
      .withBody(Json.toJson(cmd))
      .execute[Either[UpstreamErrorResponse, Unit]]
  }
}
