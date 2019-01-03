/*
 * Copyright 2019 HM Revenue & Customs
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

import config.WSHttp
import model.{BearerToken, LoginDetails, Role, SuccessfulAuthentication}
import play.api.libs.json.Json
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, HttpPost, Upstream4xxResponse }

object AuthConnector extends AuthConnector with ServicesConfig {
  override val authUrl: String = s"${baseUrl("auth")}/auth/authenticate/user"
  override val http = WSHttp
}

trait AuthConnector {

  private case class AuthResponse(access_token: BearerToken, group: Option[String], roles: Option[Set[Role]]) {
    def toAuthSuccess(loginDetails: LoginDetails) = SuccessfulAuthentication(access_token, loginDetails.userName, roles)
  }

  private object AuthResponse {
    implicit val reads = Json.reads[AuthResponse]
  }

  class InvalidCredentials extends RuntimeException("Login failed")

  val authUrl: String
  val http: HttpPost with HttpGet

  def login(loginDetails: LoginDetails)(implicit hc: HeaderCarrier): Future[SuccessfulAuthentication] =
    http.POST[LoginDetails, AuthResponse](authUrl, loginDetails)
      .map(_.toAuthSuccess(loginDetails))
      .recoverWith {
        case e: Upstream4xxResponse if e.upstreamResponseCode == 401 => Future.failed(new InvalidCredentials)
      }

  def authorized(role: Role)(implicit hc: HeaderCarrier): Future[Boolean] = authorized(role.scope, Some(role.name))

  def authorized(scope: String, role: Option[String])(implicit hc: HeaderCarrier): Future[Boolean] = {
    val authoriseUrl = role.fold(s"$authUrl/authorise?scope=$scope")(aRole => s"$authUrl/authorise?scope=$scope&role=$aRole")
    http.GET(authoriseUrl) map (_ => true) recover {
      case e: Upstream4xxResponse if e.upstreamResponseCode == 401 => false
    }
  }
}
