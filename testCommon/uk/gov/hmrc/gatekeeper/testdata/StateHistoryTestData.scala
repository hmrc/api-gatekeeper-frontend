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

import java.time.{LocalDateTime, ZoneOffset}

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{State, StateHistory}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, ApplicationId}
import uk.gov.hmrc.apiplatform.modules.common.utils.FixedClock
import uk.gov.hmrc.gatekeeper.builder.StateHistoryBuilder

trait StateHistoryTestData extends FixedClock with StateHistoryBuilder with CommonTestData {

  val stateHistories = List(
    buildStateHistory(applicationId, State.TESTING, Actors.AppCollaborator(administratorEmail), LocalDateTime.parse("2019-08-22T10:21:50.160").toInstant(ZoneOffset.UTC)),
    buildStateHistory(
      applicationId,
      State.PENDING_GATEKEEPER_APPROVAL,
      Actors.AppCollaborator(administratorEmail),
      LocalDateTime.parse("2019-08-22T10:23:10.644").toInstant(ZoneOffset.UTC)
    ),
    buildStateHistory(
      applicationId,
      State.PENDING_REQUESTER_VERIFICATION,
      Actors.GatekeeperUser("gatekeeper.username"),
      LocalDateTime.parse("2020-07-22T14:12:38.686").toInstant(ZoneOffset.UTC)
    ),
    buildStateHistory(applicationId, State.PRODUCTION, Actors.GatekeeperUser("gatekeeper.username"), LocalDateTime.parse("2020-07-22T15:12:38.686").toInstant(ZoneOffset.UTC))
  )

  val pendingApprovalStateHistory = List(
    buildStateHistory(
      pendingApprovalApplicationId,
      State.PENDING_GATEKEEPER_APPROVAL,
      Actors.AppCollaborator(administratorEmail),
      LocalDateTime.parse("2019-08-22T10:23:10.644").toInstant(ZoneOffset.UTC)
    ),
    buildStateHistory(
      pendingApprovalApplicationId,
      State.PENDING_REQUESTER_VERIFICATION,
      Actors.GatekeeperUser("gatekeeper.username"),
      LocalDateTime.parse("2020-07-22T14:12:38.686").toInstant(ZoneOffset.UTC)
    )
  )

  implicit class StateHistoryExtension(stateHistories: List[StateHistory]) {
    def toJson       = Json.toJson(stateHistories)
    def toJsonString = Json.toJson(stateHistories).toString

    def withApplicationId(applicationId: ApplicationId) = stateHistories.map(sh => sh.copy(applicationId = applicationId))
  }
}
