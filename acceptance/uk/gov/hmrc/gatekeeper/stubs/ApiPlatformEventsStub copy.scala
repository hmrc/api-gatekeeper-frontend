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
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.apiplatform.modules.events.connectors.{ApiPlatformEventsConnector, DisplayEvent, FilterValue, QueryableValues}

trait ApiPlatformEventsStub extends WireMockExtensions {

  def stubEvents(applicationId: ApplicationId, events: List[DisplayEvent]) = {
    val tags          = Set(FilterValue("Team Member", "TEAM_MEMBER"), FilterValue("Subscription", "SUBSCIPTION"))
    val actorTypes    = List(FilterValue("Application Collaborator", "COLLABORATOR"), FilterValue("Gatekeeper User", "GATEKEEPER"))
    val queryResponse = Json.stringify(Json.toJson(QueryableValues(tags.toList, actorTypes)))
    stubFor(
      get(urlMatching(s"/application-event/${applicationId.value.toString}/values"))
        .willReturn(aResponse().withBody(queryResponse).withStatus(OK))
    )

    val eventResponse = Json.stringify(Json.toJson(ApiPlatformEventsConnector.QueryResponse(events)))
    stubFor(
      get(urlMatching(s"/application-event/${applicationId.value.toString}"))
        .willReturn(aResponse().withBody(eventResponse).withStatus(OK))
    )
  }

  def stubFilteredEventsByEventTag(applicationId: ApplicationId, tag: String, events: List[DisplayEvent]) = {
    val eventResponse = Json.stringify(Json.toJson(ApiPlatformEventsConnector.QueryResponse(events)))
    stubFor(
      get(urlPathEqualTo(s"/application-event/${applicationId.value.toString}"))
        .withQueryParam("eventTag", equalTo(tag))
        .willReturn(
          aResponse().withBody(eventResponse).withStatus(OK)
        )
    )
  }

  def stubFilteredEventsByBoth(applicationId: ApplicationId, tag: String, actorType: String, events: List[DisplayEvent]) = {
    val eventResponse = Json.stringify(Json.toJson(ApiPlatformEventsConnector.QueryResponse(events)))
    stubFor(
      get(urlPathEqualTo(s"/application-event/${applicationId.value.toString}"))
        .withQueryParam("actorType", equalTo(actorType))
        .withQueryParam("eventTag", equalTo(tag))
        .willReturn(
          aResponse().withBody(eventResponse).withStatus(OK)
        )
    )
  }
}
