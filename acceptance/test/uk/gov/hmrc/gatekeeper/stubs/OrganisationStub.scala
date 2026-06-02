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
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{OrganisationId, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.apiplatform.modules.organisations.domain.models.{Organisation, OrganisationName}

trait OrganisationStub extends FixedClock {

  def stubFetchOrganisationsByUserId(userId: UserId): Unit = {
    val organisation = Organisation(OrganisationId.random, OrganisationName("Org name"), Organisation.OrganisationType.UkLimitedCompany, instant, Set.empty)

    stubFor(
      get(urlEqualTo(s"/organisation/user/${userId.value.toString}"))
        .willReturn(
          aResponse()
            .withBody(Json.stringify(Json.toJson(List(organisation))))
            .withStatus(OK)
        )
    )
  }
}
