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

package uk.gov.hmrc.gatekeeper.views.helper.application

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, CheckInformation, StateHistory, StateHistoryHelper}
import uk.gov.hmrc.gatekeeper.services.ActorSyntax._

object ViewFormat {
  val date: DateTimeFormatter                  = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm")
  def formatWithTime(instant: Instant): String = date.format(instant.atOffset(ZoneOffset.UTC))
}

object ApplicationPublicDescription {

  def apply(application: ApplicationWithCollaborators): Option[String] = {
    for {
      checkInformation <- application.details.checkInformation
      description      <- checkInformation.applicationDetails
    } yield description
  }
}

object ApplicationFormatter {
  val dateFormatter         = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  val initialLastAccessDate = LocalDateTime.of(2019, 6, 25, 0, 0) // scalastyle:ignore magic.number

  def getCreatedOn(app: ApplicationWithCollaborators): String = {
    dateFormatter.format(app.details.createdOn.atOffset(ZoneOffset.UTC))
  }

  // Caution: defaulting now = LocalDateTime.now() will not use UTC
  def getLastAccess(app: ApplicationWithCollaborators)(now: LocalDateTime): String = {
    app.details.lastAccess match {
      case Some(lastAccess) =>
        val lastAccessDate = lastAccess.atOffset(ZoneOffset.UTC).toLocalDate()
        if (ChronoUnit.SECONDS.between(app.details.createdOn, lastAccess) == 0) {
          "No API called"
        } else if (ChronoUnit.DAYS.between(initialLastAccessDate, lastAccessDate.atStartOfDay()) > 0) {
          dateFormatter.format(lastAccessDate)
        } else {
          s"More than ${ChronoUnit.MONTHS.between(lastAccessDate, now)} months ago"
        }
      case None             => "No API called"
    }
  }
}

object ApplicationSubmission {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  private def getLastSubmission(stateHistory: Seq[StateHistory]): Option[StateHistory] =
    stateHistory.filter(_.state.isPendingGatekeeperApproval)
      .sortWith(StateHistoryHelper.ascendingDateForAppId)
      .lastOption

  def getSubmittedBy(stateHistory: Seq[StateHistory]): Option[String] = {
    for {
      submission <- getLastSubmission(stateHistory)
      email      <- Some(submission.actor.id)
    } yield email
  }

  def getSubmittedOn(stateHistory: Seq[StateHistory]): Option[String] = {
    for {
      submission  <- getLastSubmission(stateHistory)
      submittedOn <- Some(dateFormatter.format(submission.changedAt.atOffset(ZoneOffset.UTC)))
    } yield submittedOn
  }
}

object ApplicationReview {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  private def getLastApproval(history: Seq[StateHistory]) =
    history.filter(_.state.isPendingRequesterVerification)
      .sortWith(StateHistoryHelper.ascendingDateForAppId)
      .lastOption

  def getApprovedOn(history: Seq[StateHistory]): Option[String] =
    getLastApproval(history).map(approval => dateFormatter.format(approval.changedAt.atOffset(ZoneOffset.UTC)))

  def getApprovedBy(history: Seq[StateHistory]): Option[String] = getLastApproval(history).map(_.actor.id)

  def getReviewContactName(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.fullname.toString()
  }

  def getReviewContactEmail(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.email.text
  }

  def getReviewContactTelephone(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.telephoneNumber
  }
}
