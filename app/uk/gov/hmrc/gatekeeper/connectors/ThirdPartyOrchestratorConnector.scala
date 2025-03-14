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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

case class ApplicationsByRequest(emails: List[LaxEmailAddress])

object ApplicationsByRequest {
  implicit val format: OFormat[ApplicationsByRequest] = Json.format[ApplicationsByRequest]
}

@Singleton
class ThirdPartyOrchestratorConnector @Inject() (http: HttpClientV2, config: ThirdPartyOrchestratorConnector.Config)(implicit ec: ExecutionContext) {

  def getApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    http.get(url"${config.serviceBaseUrl}/applications/$applicationId").execute[Option[ApplicationWithCollaborators]]
  }

  def getApplicationsByEmails(emails: List[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    http.post(url"${config.serviceBaseUrl}/developer/applications")
      .withBody(Json.toJson(ApplicationsByRequest(emails)))
      .execute[List[ApplicationWithCollaborators]]
  }

  def validateName(name: String, selfApplicationId: Option[ApplicationId], environment: Environment)(implicit hc: HeaderCarrier): Future[ApplicationNameValidationResult] = {

    val body = selfApplicationId.fold[ApplicationNameValidationRequest](NewApplicationNameValidationRequest(name))(appId => ChangeApplicationNameValidationRequest(name, appId))

    http.post(url"${config.serviceBaseUrl}/environment/$environment/application/name/validate")
      .withBody(Json.toJson[ApplicationNameValidationRequest](body))
      .execute[Option[ApplicationNameValidationResult]]
      .map {
        case Some(x) => x
        case None    => throw new RuntimeException // ApplicationNotFound
      }
  }
}

object ThirdPartyOrchestratorConnector {
  case class Config(serviceBaseUrl: String)
}
