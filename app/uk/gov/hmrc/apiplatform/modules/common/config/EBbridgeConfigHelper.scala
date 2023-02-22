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

package uk.gov.hmrc.apiplatform.modules.common.config

import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait EBbridgeConfigHelper {
  self: ServicesConfig =>

  def useProxy(serviceName: String) = getConfBool(s"$serviceName.use-proxy", defBool = false)

  def serviceUrl(key: String)(serviceName: String): String = {
    if (useProxy(serviceName)) s"${baseUrl(serviceName)}/${getConfString(s"$serviceName.context", key)}"
    else baseUrl(serviceName)
  }

  def apiKey(serviceName: String) = getConfString(s"$serviceName.api-key", "")

  def bearerToken(serviceName: String) = getConfString(s"$serviceName.bearer-token", "")
}
