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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, CheckInformation, StateHistory}
import uk.gov.hmrc.gatekeeper.services.ActorSyntax._

object ApplicationPublicDescription {

  def apply(application: ApplicationResponse): Option[String] = {
    for {
      checkInformation <- application.checkInformation
      description      <- checkInformation.applicationDetails
    } yield description
  }
}

object ApplicationFormatter {
  val dateFormatter         = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  val initialLastAccessDate = LocalDateTime.of(2019, 6, 25, 0, 0) // scalastyle:ignore magic.number

  def getCreatedOn(app: ApplicationResponse): String = {
    dateFormatter.format(app.createdOn)
  }

  // Caution: defaulting now = LocalDateTime.now() will not use UTC
  def getLastAccess(app: ApplicationResponse)(now: LocalDateTime): String = {
    app.lastAccess match {
      case Some(lastAccess) =>
        if (ChronoUnit.SECONDS.between(app.createdOn, lastAccess) == 0) {
          "No API called"
        } else if (ChronoUnit.DAYS.between(initialLastAccessDate.toLocalDate, lastAccess.toLocalDate) > 0) {
          dateFormatter.format(lastAccess)
        } else {
          s"More than ${ChronoUnit.MONTHS.between(lastAccess, now)} months ago"
        }
      case None             => "No API called"
    }

  }
}

object ApplicationSubmission {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  private def getLastSubmission(stateHistory: Seq[StateHistory]): Option[StateHistory] =
    stateHistory.filter(_.state.isPendingGatekeeperApproval)
      .sortWith(StateHistory.ascendingDateForAppId)
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
      submittedOn <- Some(dateFormatter.format(submission.changedAt))
    } yield submittedOn
  }
}

object ApplicationReview {
  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  private def getLastApproval(history: Seq[StateHistory]) =
    history.filter(_.state.isPendingRequesterVerification)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption

  def getApprovedOn(history: Seq[StateHistory]): Option[String] =
    getLastApproval(history).map(approval => dateFormatter.format(approval.changedAt))

  def getApprovedBy(history: Seq[StateHistory]): Option[String] = getLastApproval(history).map(_.actor.id)

  def getReviewContactName(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.fullname
  }

  def getReviewContactEmail(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.email
  }

  def getReviewContactTelephone(checkInformationOpt: Option[CheckInformation]): Option[String] = {
    for {
      checkInformation <- checkInformationOpt
      contactDetails   <- checkInformation.contactDetails
    } yield contactDetails.telephoneNumber
  }
}
