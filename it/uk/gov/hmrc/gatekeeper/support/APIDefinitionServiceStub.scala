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
import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
import uk.gov.hmrc.gatekeeper.models.{APICategoryDetails, ApiDefinitionGK}
import play.api.http.Status
import play.api.libs.json.Json

trait APIDefinitionServiceStub {
  val apiPublicDefinitionUrl  = "/api-definition"
  val apiPrivateDefinitionUrl = "/api-definition?type=private"
  val getCategoriesUrl        = "/api-categories"

  def primeDefinitionServiceSuccessWithPublicAPIs(apis: Seq[ApiDefinitionGK]): Unit = {
    stubFor(get(urlEqualTo(apiPublicDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())
      ))
  }

  def primeDefinitionServiceSuccessWithPrivateAPIs(apis: Seq[ApiDefinitionGK]): Unit = {
    stubFor(get(urlEqualTo(apiPrivateDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())
      ))
  }

  def primeGetAllCategories(apis: Seq[APICategoryDetails]): Unit = {
    stubFor(get(urlEqualTo(getCategoriesUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())
      ))
  }
}
