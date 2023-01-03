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

import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.models.State.State
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

case class Actor(id: String)

case class StateHistory(applicationId: ApplicationId, state: State, actor: Actor, notes: Option[String] = None, changedAt: DateTime = DateTime.now())

object StateHistory {

  def ascendingDateForAppId(s1: StateHistory, s2: StateHistory): Boolean = {
    s1.applicationId match {
      case s2.applicationId => s1.changedAt.isBefore(s2.changedAt)
      case _                => true
    }
  }

  implicit val formatState = Json.formatEnum(State)
  implicit val formatActor = Json.format[Actor]
  implicit val format      = Json.format[StateHistory]
}
