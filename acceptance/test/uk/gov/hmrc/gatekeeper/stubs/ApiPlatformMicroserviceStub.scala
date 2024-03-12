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
import uk.gov.hmrc.gatekeeper.testdata.ApiDefinitionTestData

trait ApiPlatformMicroserviceStub extends WireMockExtensions with ApiDefinitionTestData {

  def stubNewApplication(application: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/applications/${appId}")).willReturn(aResponse().withBody(application).withStatus(OK)))
  }

  def stubApiDefintionsForApplication(apiDefinitions: String, appId: ApplicationId) = {
    stubFor(get(urlEqualTo(s"/api-definitions?applicationId=${appId}&restricted=false")).willReturn(aResponse().withBody(apiDefinitions).withStatus(OK)))
  }

  def stubApiDefinition() = {
    stubFor(get(urlPathEqualTo("/api-definitions/all")).willReturn(aResponse().withStatus(OK).withBody(mapOfApiDefinitions)))
    stubFor(get(urlPathEqualTo("/api-definitions/nonopen")).willReturn(aResponse().withStatus(OK).withBody(listOfApiDefinitions)))
  }
}
