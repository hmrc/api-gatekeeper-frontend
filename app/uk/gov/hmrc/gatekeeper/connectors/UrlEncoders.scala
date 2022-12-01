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

import java.net.URLEncoder.encode
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.gatekeeper.models.ClientId

trait UrlEncoders {
  implicit class UrlEncodeContext(context: ApiContext) {
    def urlEncode: String = encode(context.value, "UTF-8")
  }
  implicit class UrlEncodeVersion(version: ApiVersion) {
    def urlEncode: String = encode(version.value, "UTF-8")
  }
  implicit class UrlEncodeClientId(clientId: ClientId) {
    def urlEncode: String = encode(clientId.value, "UTF-8")
  }
}
