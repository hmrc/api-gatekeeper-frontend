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

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models.RegisteredUser

trait ApplicationServiceStub extends WireMockExtensions {

  def primeApplicationServiceSuccessWithUsers(users: Seq[RegisteredUser]): Unit = {
    val request = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("api1"), ApiVersionNbr("1.0"))

    stubFor(post(urlEqualTo("/collaborators"))
      .withJsonRequestBody(request)
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withJsonBody(users.map(_.email))
      ))
  }

  def primeApplicationServiceFetchApplicationBySubscription(definition: ApiDefinition, apps: List[ApplicationWithCollaborators]): Unit = {

    stubFor(get(urlPathEqualTo("/application"))
      .withQueryParam("subscribesTo", containing(definition.context.value))
      .withQueryParam("version", containing(definition.versions.head._1.value))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withJsonBody(apps)
      ))
  }
}
