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

package uk.gov.hmrc.gatekeeper.models

import java.time.LocalDateTime

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.State.State
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

case class ApplicationStateHistoryItem(state: State, timestamp: LocalDateTime)
case class ApplicationStateHistory(applicationId: ApplicationId, appName: String, journeyVersion: Int, stateHistory: List[ApplicationStateHistoryItem])

object ApplicationStateHistory {
  implicit val formatApplicationStateHistoryItem: OFormat[ApplicationStateHistoryItem] = Json.format[ApplicationStateHistoryItem]
  implicit val formatApplicationStateHistory: OFormat[ApplicationStateHistory]         = Json.format[ApplicationStateHistory]
}
