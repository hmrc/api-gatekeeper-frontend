/*
 * Copyright 2020 HM Revenue & Customs
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

package model

import org.joda.time.DateTime
import play.api.libs.json.Json
import model.State.State
import uk.gov.hmrc.time.DateTimeUtils
import play.api.libs.json.JodaReads._
import play.api.libs.json.JodaWrites._

case class Actor(id: String)

case class StateHistory(applicationId: ApplicationId,
                        state: State,
                        actor: Actor,
                        notes: Option[String] = None,
                        changedAt: DateTime = DateTimeUtils.now)

object StateHistory {

  def ascendingDateForAppId(s1:StateHistory, s2:StateHistory): Boolean = {
    s1.applicationId match {
      case s2.applicationId => s1.changedAt.isBefore(s2.changedAt)
      case _ => true
    }
  }

  implicit val formatApplicationId = Json.valueFormat[ApplicationId]
  implicit val formatState = EnumJson.enumFormat(State)
  implicit val formatActor = Json.format[Actor]
  implicit val format = Json.format[StateHistory]
}
