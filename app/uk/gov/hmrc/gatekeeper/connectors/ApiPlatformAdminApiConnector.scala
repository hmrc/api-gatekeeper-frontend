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
import uk.gov.hmrc.http.{Authorization, HeaderCarrier, HttpClient}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithUsers

@Singleton
class ApiPlatformAdminApiConnector @Inject() (http: HttpClient, config: ApiPlatformAdminApiConnector.Config)(implicit ec: ExecutionContext) {

  def getApplication(applicationId: ApplicationId, hc: HeaderCarrier): Future[Option[ApplicationWithUsers]] = {
    implicit val headerCarrier: HeaderCarrier = hc.copy(authorization = Some(Authorization(config.authToken)))
    http.GET[Option[ApplicationWithUsers]](url = s"${config.serviceBaseUrl}/applications/$applicationId")
  }
}

object ApiPlatformAdminApiConnector {
  case class Config(serviceBaseUrl: String, authToken: String)
}
