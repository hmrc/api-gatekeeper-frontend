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

package uk.gov.hmrc.apiplatform.modules.gkauth.config

import javax.inject.{Inject, Provider, Singleton}
import play.api.Configuration

case class StrideAuthRoles(
    adminRole: String,
    superUserRole: String,
    userRole: String
  )

case class StrideAuthConfig(
    strideLoginUrl: String,
    successUrl: String,
    origin: String,
    roles: StrideAuthRoles
  )

@Singleton
class StrideAuthConfigProvider @Inject() (configuration: Configuration) extends Provider[StrideAuthConfig] with BaseUrlExtractor {
  val config = configuration.underlying

  override def get(): StrideAuthConfig = {
    val strideLoginUrl = s"${extractBaseUrl("stride-auth-frontend")}/stride/sign-in"

    val strideConfig  = configuration.underlying.getConfig("stride")
    val successUrl    = strideConfig.getString("success-url")
    val origin        = strideConfig.getString("origin")
    val adminRole     = strideConfig.getString("roles.admin")
    val superUserRole = strideConfig.getString("roles.super-user")
    val userRole      = strideConfig.getString("roles.user")

    StrideAuthConfig(strideLoginUrl, successUrl, origin, StrideAuthRoles(adminRole, superUserRole, userRole))
  }
}
