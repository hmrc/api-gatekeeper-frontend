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

package connectors

import javax.inject.{Inject, Singleton}
import model.ApiContext
import uk.gov.hmrc.http.HeaderCarrier

import model.applications._
import scala.concurrent.Future

import uk.gov.hmrc.http.HttpClient

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
import model.ApplicationUpdateSuccessResult
import model.AddTeamMemberRequest
import model.TeamMemberAlreadyExists
import model.ApplicationNotFound
import uk.gov.hmrc.http.UpstreamErrorResponse
import play.api.http.Status._
import uk.gov.hmrc.http.HttpReads.Implicits._
import model.CombinedApi

@Singleton
class ApmConnector @Inject() (http: HttpClient, config: ApmConnector.Config)(implicit ec: ExecutionContext) {
  import ApmConnectorJsonFormatters._
  import ApmConnector._

  def fetchApplicationById(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithSubscriptionData]] =
    http.GET[Option[ApplicationWithSubscriptionData]](s"${config.serviceBaseUrl}/applications/${applicationId.value}")

  def getAllFieldDefinitions(environment: Environment)(implicit hc: HeaderCarrier): Future[ApiDefinitions.Alias] = {
    http.GET[Map[ApiContext, Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]]]](s"${config.serviceBaseUrl}/subscription-fields?environment=$environment")
  }
  
  def addTeamMember(applicationId: ApplicationId, addTeamMember: AddTeamMemberRequest)(implicit hc: HeaderCarrier): Future[Unit] = {

    http.POST[AddTeamMemberRequest, Either[UpstreamErrorResponse, Unit]](s"${config.serviceBaseUrl}/applications/${applicationId.value}/collaborators", addTeamMember)
    .map( _ match {
      case Right(()) => ()
      case Left(UpstreamErrorResponse(_, CONFLICT, _, _)) => throw TeamMemberAlreadyExists
      case Left(UpstreamErrorResponse(_, NOT_FOUND, _, _)) => throw ApplicationNotFound
      case Left(err) => throw err
    })
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
    http.POST[ApiIdentifier, Either[UpstreamErrorResponse, Unit]](
      s"${config.serviceBaseUrl}/applications/${applicationId.value}/subscriptions?restricted=false",
      apiIdentifier
    )
    .map(_ match {
      case Right(_) => ApplicationUpdateSuccessResult
      case Left(err) => throw err
    })
  }

  def fetchAllCombinedApis()(implicit hc: HeaderCarrier): Future[List[CombinedApi]] = {
    http.GET[List[CombinedApi]](s"${config.serviceBaseUrl}/combined-rest-xml-apis")
  }

  // TODO: Test me
  def fetchAllBoxes()(implicit hc: HeaderCarrier): Future[List[model.Box]] = {
    http.GET[List[model.Box]](s"${config.serviceBaseUrl}/push-pull-notifications/box")
  }
}

object ApmConnector {
  val applicationIdQueryParam = "applicationId"
  val restrictedQueryParam = "restricted"

  case class Config(
      serviceBaseUrl: String
  )
}
