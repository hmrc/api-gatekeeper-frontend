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

package uk.gov.hmrc.gatekeeper.connectors

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.applications._
import scala.concurrent.Future

import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData
import uk.gov.hmrc.gatekeeper.models.pushpullnotifications.Box
import uk.gov.hmrc.gatekeeper.models.Environment.Environment
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition
import uk.gov.hmrc.http.UpstreamErrorResponse
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config)(implicit ec: ExecutionContext) extends APIDefinitionFormatters
  with ApplicationUpdateFormatters {

  import ApmConnectorJsonFormatters._
  import ApmConnector._

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] =
    http.GET[Option[ApplicationWithSubscriptionData]](s"${config.serviceBaseUrl}/applications/${applicationId.value}")

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias] = {
    http.GET[Map[ApiContext, Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]]]](s"${config.serviceBaseUrl}/subscription-fields?environment=$environment")
  }
  
  def addTeamMember(applicationId: ApplicationId, addTeamMember: AddTeamMemberRequest)(implicit hc: HeaderCarrier): Future[Unit] = {

    http.POST[AddTeamMemberRequest, Either[UpstreamErrorResponse, Unit]](s"${config.serviceBaseUrl}/applications/${applicationId.value}/collaborators", addTeamMember)
      .map {
        case Right(()) => ()
        case Left(UpstreamErrorResponse(_, CONFLICT, _, _)) => throw TeamMemberAlreadyExists
        case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => throw ApplicationNotFound
        case Left(err) => throw err
      }
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

  def updateApplication(applicationId: ApplicationId, request: ApplicationUpdate)(implicit hc: HeaderCarrier): Future[NewApplication] = {
    http.PATCH[ApplicationUpdate, NewApplication](s"${config.serviceBaseUrl}/applications/${applicationId.value}", request)
  }
  
  def subscribeToApi(applicationId: ApplicationId, subscribeToApi: SubscribeToApi)(implicit hc: HeaderCarrier): Future[ApplicationUpdateResult] = {
    http.POST[SubscribeToApi, Either[UpstreamErrorResponse, Unit]](
      s"${config.serviceBaseUrl}/applications/${applicationId.value}/subscriptionsAppUpdate?restricted=false",
      subscribeToApi
    ).map {
      case Right(_) => ApplicationUpdateSuccessResult
      case Left(err) => throw err
    }
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.GET[List[CombinedApi]](s"${config.serviceBaseUrl}/combined-rest-xml-apis")
  }

  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[Box]] = {
    http.GET[List[Box]](s"${config.serviceBaseUrl}/push-pull-notifications/boxes")
  }
}

object ApmConnector {
  val applicationIdQueryParam = "applicationId"
  val restrictedQueryParam = "restricted"

  case class Config(
      serviceBaseUrl: String
  )
}
