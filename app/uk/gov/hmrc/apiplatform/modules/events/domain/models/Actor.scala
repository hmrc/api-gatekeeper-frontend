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

package uk.gov.hmrc.apiplatform.modules.events.domain.models

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}

sealed trait ActorType extends EnumEntry

object ActorType extends Enum[ActorType] with PlayJsonEnum[ActorType] {
  val values: scala.collection.immutable.IndexedSeq[ActorType] = findValues

  case object COLLABORATOR extends ActorType
  case object GATEKEEPER extends  ActorType
  case object SCHEDULED_JOB extends ActorType
  case object UNKNOWN extends ActorType
}

case class OldActor(id: String, actorType: ActorType)

sealed trait Actor

object Actor {
  def extractActorText(actor: Actor): String = actor match {
    case GatekeeperUserActor(user) => user
    case CollaboratorActor(email) => email
  }
}

case class GatekeeperUserActor(user: String) extends Actor
case class CollaboratorActor(email: String) extends Actor
