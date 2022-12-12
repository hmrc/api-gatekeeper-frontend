/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.builder.ApplicationResponseBuilder
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models.AbstractApplicationEvent

trait ApplicationEventsTestData extends ApplicationResponseBuilder with CollaboratorsTestData with AccessTestData with ApplicationEventTestDataBuilder with ApplicationStateTestData {

  val event1 = makeTeamMemberAddedEvent(applicationId, 1)
  val event2 = makeTeamMemberAddedEvent(applicationId, 2).copy(eventDateTime = event1.eventDateTime.minusMinutes(1))
  val event3 = makeTeamMemberRemovedEvent(applicationId, 2).copy(eventDateTime = event2.eventDateTime.minusMinutes(1))
  val event4 = makeApiSubscribed(applicationId, 1).copy(eventDateTime = event3.eventDateTime.minusMinutes(1))

  def makeSomeEvents(applicationId: ApplicationId) = {
    List[AbstractApplicationEvent](event1, event2, event3, event4)
  }
}
