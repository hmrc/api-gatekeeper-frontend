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

package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition

trait APIDefinitionServiceStub {
  val apmDefinitionsUrl = "/api-definitions/all"

  def primeDefinitionServiceSuccessWithAPIs(apis: Seq[ApiDefinition]): Unit = {
    stubFor(get(urlPathEqualTo(apmDefinitionsUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())
      ))
  }
}
