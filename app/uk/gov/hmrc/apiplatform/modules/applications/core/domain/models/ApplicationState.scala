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

package uk.gov.hmrc.apiplatform.modules.applications.core.domain.models

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.LocalDateTime
import java.{util => ju}

class InvalidStateTransition(invalidFrom: State, to: State, expectedFrom: State)
    extends RuntimeException(s"Transition to '$to' state requires the application to be in '$expectedFrom' state, but it was in '$invalidFrom'")

case class ApplicationState(
    name: State = State.TESTING,
    requestedByEmailAddress: Option[String] = None,
    requestedByName: Option[String] = None,
    verificationCode: Option[String] = None,
    updatedOn: LocalDateTime
  ) {

  final def requireState(requirement: State, transitionTo: State): Unit = {
    if (name != requirement) {
      throw new InvalidStateTransition(expectedFrom = requirement, invalidFrom = name, to = transitionTo)
    }
  }

  lazy val isInTesting                                                      = name == State.TESTING
  lazy val isPendingResponsibleIndividualVerification                       = name == State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION
  lazy val isPendingGatekeeperApproval                                      = name == State.PENDING_GATEKEEPER_APPROVAL
  lazy val isPendingRequesterVerification                                   = name == State.PENDING_REQUESTER_VERIFICATION
  lazy val isInPreProductionOrProduction                                    = name == State.PRE_PRODUCTION || name == State.PRODUCTION
  lazy val isInPendingGatekeeperApprovalOrResponsibleIndividualVerification = name == State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION || name == State.PENDING_GATEKEEPER_APPROVAL
  lazy val isInProduction                                                   = name == State.PRODUCTION
  lazy val isDeleted                                                        = name == State.DELETED

  def toProduction(timestamp: LocalDateTime) = {
    requireState(requirement = State.PRE_PRODUCTION, transitionTo = State.PRODUCTION)
    copy(name = State.PRODUCTION, updatedOn = timestamp)
  }

  def toPreProduction(timestamp: LocalDateTime) = {
    requireState(requirement = State.PENDING_REQUESTER_VERIFICATION, transitionTo = State.PRE_PRODUCTION)
    copy(name = State.PRE_PRODUCTION, updatedOn = timestamp)
  }

  def toTesting(timestamp: LocalDateTime) = copy(name = State.TESTING, requestedByEmailAddress = None, requestedByName = None, verificationCode = None, updatedOn = timestamp)

  def toPendingGatekeeperApproval(requestedByEmailAddress: String, requestedByName: String, timestamp: LocalDateTime) = {
    requireState(requirement = State.TESTING, transitionTo = State.PENDING_GATEKEEPER_APPROVAL)

    copy(
      name = State.PENDING_GATEKEEPER_APPROVAL,
      updatedOn = timestamp,
      requestedByEmailAddress = Some(requestedByEmailAddress),
      requestedByName = Some(requestedByName)
    )
  }

  def toPendingResponsibleIndividualVerification(requestedByEmailAddress: String, requestedByName: String, timestamp: LocalDateTime) = {
    requireState(requirement = State.TESTING, transitionTo = State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION)

    copy(
      name = State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION,
      updatedOn = timestamp,
      requestedByEmailAddress = Some(requestedByEmailAddress),
      requestedByName = Some(requestedByName)
    )
  }

  def toPendingRequesterVerification(timestamp: LocalDateTime) = {
    requireState(requirement = State.PENDING_GATEKEEPER_APPROVAL, transitionTo = State.PENDING_REQUESTER_VERIFICATION)

    def verificationCode(input: String = ju.UUID.randomUUID().toString): String = {
      def urlSafe(encoded: String) = encoded.replace("=", "").replace("/", "_").replace("+", "-")

      val digest = MessageDigest.getInstance("SHA-256")
      urlSafe(new String(ju.Base64.getEncoder.encode(digest.digest(input.getBytes(StandardCharsets.UTF_8))), StandardCharsets.UTF_8))
    }
    copy(name = State.PENDING_REQUESTER_VERIFICATION, verificationCode = Some(verificationCode()), updatedOn = timestamp)
  }

  def toDeleted(timestamp: LocalDateTime) = copy(name = State.DELETED, verificationCode = None, updatedOn = timestamp)
}

object ApplicationState {
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.LocalDateTimeFormatter._
  import play.api.libs.json._

  implicit val formatApplicationState: OFormat[ApplicationState] = Json.format[ApplicationState]
}
