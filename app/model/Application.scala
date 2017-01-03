/*
 * Copyright 2017 HM Revenue & Customs
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

import java.util.UUID

import model.CollaboratorRole.CollaboratorRole
import model.State.State
import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.time.DateTimeUtils

trait Application {
  val id: UUID
  val name: String
  val state: ApplicationState
  val collaborators: Set[Collaborator]
}


case class ApplicationResponse(id: UUID,
                               name: String,
                               description: Option[String] = None,
                               collaborators: Set[Collaborator],
                               createdOn: DateTime,
                               state: ApplicationState) extends Application {

  def admins = collaborators.filter(_.role == CollaboratorRole.ADMINISTRATOR)
}

object ApplicationResponse {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = EnumJson.enumFormat(State)
  implicit val format4 = Json.format[ApplicationState]
  implicit val format5 = Json.format[ApplicationResponse]
}

case class SubscribedApplicationResponse(id: UUID,
                                         name: String,
                                         description: Option[String] = None,
                                         collaborators: Set[Collaborator],
                                         createdOn: DateTime,
                                         state: ApplicationState,
                                         subscriptionNames: Seq[String]) extends Application


object SubscribedApplicationResponse {
  implicit val format1 = Json.format[APIIdentifier]
  implicit val formatRole = EnumJson.enumFormat(CollaboratorRole)
  implicit val format2 = Json.format[Collaborator]
  implicit val format3 = EnumJson.enumFormat(State)
  implicit val format4 = Json.format[ApplicationState]
  implicit val format5 = Json.format[SubscribedApplicationResponse]

  def createFrom(appResponse: ApplicationResponse, subscriptions: Seq[String]) =
    SubscribedApplicationResponse(appResponse.id, appResponse.name, appResponse.description,
      appResponse.collaborators, appResponse.createdOn, appResponse.state, subscriptions)
}

object State extends Enumeration {
  type State = Value
  val TESTING, PENDING_GATEKEEPER_APPROVAL, PENDING_REQUESTER_VERIFICATION, PRODUCTION = Value
  implicit val format = EnumJson.enumFormat(State)
}

object CollaboratorRole extends Enumeration {
  type CollaboratorRole = Value
  val DEVELOPER, ADMINISTRATOR = Value
}

case class Collaborator(emailAddress: String, role: CollaboratorRole)

case class ApplicationState(name: State = State.TESTING, requestedByEmailAddress: Option[String] = None,
                            verificationCode: Option[String] = None, updatedOn: DateTime = DateTimeUtils.now)
