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

package uk.gov.hmrc.apiplatform.modules.events.connectors

import scala.concurrent.Future.successful

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, Environment}
import uk.gov.hmrc.apiplatform.modules.common.utils._

class EnvironmentAwareApiPlatformEventsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with FixedClock {

  trait Setup {
    val subordinate: SubordinateApiPlatformEventsConnector = mock[SubordinateApiPlatformEventsConnector]
    val principal: PrincipalApiPlatformEventsConnector     = mock[PrincipalApiPlatformEventsConnector]

    val emptyValues: QueryableValues = QueryableValues(List.empty, List.empty)
    when(subordinate.fetchQueryableValues(*[ApplicationId])(*)).thenReturn(successful(emptyValues))
    when(principal.fetchQueryableValues(*[ApplicationId])(*)).thenReturn(successful(emptyValues))

    val connector = new EnvironmentAwareApiPlatformEventsConnector(subordinate, principal)

    val appId = ApplicationId.random

    implicit val hc: HeaderCarrier = HeaderCarrier()
  }

  "Call subordinate when environment is SANDBOX" in new Setup {

    await(connector.fetchQueryableValues(appId, Environment.SANDBOX))

    verify(subordinate, times(1)).fetchQueryableValues(eqTo(appId))(*)
    verify(principal, never).fetchQueryableValues(*[ApplicationId])(*)
  }

  "Call principal when environment is PRODUCTION" in new Setup {

    await(connector.fetchQueryableValues(appId, Environment.PRODUCTION))

    verify(subordinate, never).fetchQueryableValues(eqTo(appId))(*)
    verify(principal, times(1)).fetchQueryableValues(*[ApplicationId])(*)
  }
}
