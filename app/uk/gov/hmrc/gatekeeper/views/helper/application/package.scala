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

package uk.gov.hmrc.gatekeeper.views.helper.application

import uk.gov.hmrc.gatekeeper.models._
import org.joda.time.DateTime
import org.joda.time.Days.daysBetween
import org.joda.time.Months.monthsBetween
import org.joda.time.Seconds.secondsBetween
import org.joda.time.format.DateTimeFormat
import uk.gov.hmrc.gatekeeper.models.applications.NewApplication

object ApplicationPublicDescription {

  def apply(application: NewApplication): Option[String] = {
    for {
      checkInformation <- application.checkInformation
      description      <- checkInformation.applicationDetails
    } yield description
  }
}

object ApplicationFormatter {
  val dateFormatter         = DateTimeFormat.forPattern("dd MMMM yyyy")
  val initialLastAccessDate = new DateTime(2019, 6, 25, 0, 0) // scalastyle:ignore magic.number

  def getCreatedOn(app: NewApplication): String = {
    dateFormatter.print(app.createdOn)
  }

  def getLastAccess(app: NewApplication): String = {
    app.lastAccess match {
      case Some(lastAccess) =>
        if (secondsBetween(app.createdOn, lastAccess).getSeconds == 0) {
          "No API called"
        } else if (daysBetween(initialLastAccessDate.toLocalDate, lastAccess.toLocalDate).getDays > 0) {
          dateFormatter.print(lastAccess)
        } else {
          s"More than ${monthsBetween(lastAccess, DateTime.now()).getMonths} months ago"
        }
      case None             => "No API called"
    }

  }
}

object ApplicationSubmission {
  val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

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
      submittedOn <- Some(dateFormatter.print(submission.changedAt))
    } yield submittedOn
  }
}

object ApplicationReview {
  val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

  private def getLastApproval(history: Seq[StateHistory]) =
    history.filter(_.state.isPendingRequesterVerification)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption

  def getApprovedOn(history: Seq[StateHistory]): Option[String] =
    getLastApproval(history).map(approval => dateFormatter.print(approval.changedAt))

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
