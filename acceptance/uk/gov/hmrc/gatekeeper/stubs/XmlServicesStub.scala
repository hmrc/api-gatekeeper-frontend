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

package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import play.api.http.Status.OK
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar.xmlApis

trait XmlServicesStub {


  def stubGetAllXmlApis(): Unit = {
    stubFor(get(urlEqualTo("/api-platform-xml-services/xml/apis"))
      .willReturn(aResponse().withBody(xmlApis).withStatus(OK)))
  }

  def stubGetXmlOrganisationsForUser(userId: UserId): Unit = {
    stubFor(get(urlEqualTo(s"/api-platform-xml-services/organisations?userId=${userId.value}&sortBy=ORGANISATION_NAME"))
      .willReturn(aResponse().withBody("[]").withStatus(OK)))
  }

  def stubGetXmlApiForCategories(): Unit = {
    stubFor(get(urlEqualTo("/api-platform-xml-services/xml/apis/filtered"))
      .willReturn(aResponse().withBody(xmlApis).withStatus(OK)))
  }


}
