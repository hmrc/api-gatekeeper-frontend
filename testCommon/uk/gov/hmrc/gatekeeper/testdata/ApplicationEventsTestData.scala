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

package uk.gov.hmrc.gatekeeper.testdata

import java.time.temporal.ChronoUnit

import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models.ApplicationEvent

trait ApplicationEventsTestData  extends ApplicationEventTestDataBuilder with CommonTestData {

  val event1 = makeTeamMemberAddedEvent(applicationId,1)
  val event2 = makeTeamMemberAddedEvent(applicationId,2).copy(eventDateTime = event1.eventDateTime.minus(1, ChronoUnit.MINUTES))
  val event3 = makeTeamMemberRemovedEvent(applicationId, 2).copy(eventDateTime = event2.eventDateTime.minus(1, ChronoUnit.MINUTES))
  val event4 = makeApiSubscribedV2(applicationId).copy(eventDateTime = event3.eventDateTime.minus(1, ChronoUnit.MINUTES))

  def makeSomeEvents() = {
    List[ApplicationEvent](event1, event2, event3, event4)
  }
}
