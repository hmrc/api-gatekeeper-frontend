/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar

trait ThirdPartyApplicationStub extends WireMockExtensions {
    import MockDataSugar._

  def stubApplicationList(body: String): Unit = {
    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse()
        .withBody(body)
        .withStatus(OK)))
  }

  def stubApplication(applicationResponse: String): Unit = {
    stubFor(get(urlEqualTo("/application"))
      .willReturn(aResponse()
      .withBody(applicationResponse)
        .withStatus(OK)))
  }

  def stubApplicationSubscription(applicationSubscriptionResponse: String): Unit = {
    stubFor(
      get(
        urlEqualTo("/application/subscriptions")
      )
      .willReturn(aResponse()
        .withBody(applicationSubscriptionResponse)
        .withStatus(OK)))
  }

  def stubApplicationSubscription() = {
    stubFor(
      get(
        urlEqualTo("/application/subscriptions")
      )
      .willReturn(
        aResponse()
          .withBody(MockDataSugar.applicationSubscription)
          .withStatus(OK)
      )
    )
  }

  def stubApiSubscription(apiContext: String) = {
    stubFor(get(urlEqualTo(s"/application?subscribesTo=$apiContext"))
      .willReturn(aResponse().withBody(applicationResponse).withStatus(OK)))
  }

  def stubApplicationsCollaborators(developers: Seq[User]): Unit = {
    val developersJson = developers.map(u => u.email)
    val request = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("employers-paye"), ApiVersionNbr("1.0"), Some("partialEmail"))
  
    stubFor(post(urlEqualTo("/collaborators"))
      .withJsonRequestBody(request)
      .willReturn(aResponse()
        .withJsonBody(developersJson)
        .withStatus(OK)))
  }
}
