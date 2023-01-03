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

import uk.gov.hmrc.apiplatform.modules.common.utils._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import uk.gov.hmrc.gatekeeper.testdata.ApplicationEventsTestData
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.Future.successful

class EnvironmentAwareApiPlatformEventsConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with ApplicationEventsTestData {

  trait Setup {
    val subordinate: SubordinateApiPlatformEventsConnector = mock[SubordinateApiPlatformEventsConnector]
    val principal: PrincipalApiPlatformEventsConnector     = mock[PrincipalApiPlatformEventsConnector]

    when(subordinate.fetchQueryableEventTags(*[ApplicationId])(*)).thenReturn(successful(List.empty))
    when(principal.fetchQueryableEventTags(*[ApplicationId])(*)).thenReturn(successful(List.empty))

    val connector = new EnvironmentAwareApiPlatformEventsConnector(subordinate, principal)

    val appId = ApplicationId.random

    implicit val hc = HeaderCarrier()
  }

  "Call subordinate when environment is SANDBOX" in new Setup {

    await(connector.fetchQueryableEventTags(appId, "SANDBOX"))

    verify(subordinate, times(1)).fetchQueryableEventTags(eqTo(appId))(*)
    verify(principal, never).fetchQueryableEventTags(*[ApplicationId])(*)
  }

  "Call principal when environment is PRODUCTION" in new Setup {

    await(connector.fetchQueryableEventTags(appId, "PRODUCTION"))

    verify(subordinate, never).fetchQueryableEventTags(eqTo(appId))(*)
    verify(principal, times(1)).fetchQueryableEventTags(*[ApplicationId])(*)
  }
}
