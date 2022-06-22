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

package uk.gov.hmrc.modules.stride.config

import javax.inject.{Inject, Provider, Singleton}
import play.api.Configuration
import com.typesafe.config.Config

case class StrideAuthConfig(
  authBaseUrl: String,
  strideLoginUrl: String,
  successUrl: String,
  origin: String,
  adminRole: String,
  superUserRole: String,
  userRole: String
)

trait BaseUrl {
  def config: Config

  protected lazy val rootServices = "microservice.services"

  def baseUrl(serviceName: String) = {
    val protocol = config.getString(s"${rootServices}.$serviceName.protocol")
    val host     = config.getString(s"${rootServices}.$serviceName.host")
    val port     = config.getString(s"${rootServices}.$serviceName.port")
    s"$protocol://$host:$port"
  }
}

@Singleton
class StrideAuthConfigProvider @Inject()(configuration: Configuration) extends Provider[StrideAuthConfig] with BaseUrl {
  val config = configuration.underlying

  override def get(): StrideAuthConfig = {
    val authBaseUrl = baseUrl("auth")
    val strideLoginUrl = s"${baseUrl("stride-auth-frontend")}/stride/sign-in"
    
    val strideConfig = configuration.underlying.getConfig("stride")
    val successUrl = strideConfig.getString("success-url")
    val origin = strideConfig.getString("origin")
    val adminRole = strideConfig.getString("roles.admin")
    val superUserRole = strideConfig.getString("roles.super-user")
    val userRole = strideConfig.getString("roles.user")

    StrideAuthConfig(authBaseUrl, strideLoginUrl, successUrl, origin, adminRole, superUserRole, userRole)
  }
}
