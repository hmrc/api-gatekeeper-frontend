/*
 * Copyright 2018 HM Revenue & Customs
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

package views.helper.application

import model._
import org.joda.time.format.DateTimeFormat

object ApplicationPublicDescription {
  def apply(application: ApplicationResponse): Option[String] = {
    for {
      checkInformation <- application.checkInformation
      description <- checkInformation.applicationDetails
    } yield description
  }
}

object ApplicationSubmission {
  val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

  private def getLastSubmission(application: ApplicationWithHistory): Option[StateHistory] =
    application.history.filter(_.state == State.PENDING_GATEKEEPER_APPROVAL)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption


  def getSubmittedBy(application: ApplicationWithHistory): Option[String] = {
    for {
      submission <- getLastSubmission(application)
      email <- Some(submission.actor.id)
    } yield email
  }

  def getSubmittedOn(application: ApplicationWithHistory): Option[String] = {
    for {
      submission <- getLastSubmission(application)
      submittedOn <- Some(dateFormatter.print(submission.changedAt))
    } yield submittedOn
  }
}

object ApplicationReview {
  val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

  private def getLastApproval(app: ApplicationWithHistory) =
    app.history.filter(_.state == State.PENDING_REQUESTER_VERIFICATION)
      .sortWith(StateHistory.ascendingDateForAppId)
      .lastOption

  def getApprovedOn(app: ApplicationWithHistory): Option[String] =
    getLastApproval(app).map(approval => dateFormatter.print(approval.changedAt))

  def getApprovedBy(app: ApplicationWithHistory): Option[String] = getLastApproval(app).map(_.actor.id)

  def getReviewContactName(app: ApplicationResponse): Option[String] = {
    for {
      checkInformation <- app.checkInformation
      contactDetails <- checkInformation.contactDetails
    } yield contactDetails.fullname
  }

  def getReviewContactEmail(app: ApplicationResponse): Option[String] = {
    for {
      checkInformation <- app.checkInformation
      contactDetails <- checkInformation.contactDetails
    } yield contactDetails.email
  }

  def getReviewContactTelephone(app: ApplicationResponse): Option[String] = {
    for {
      checkInformation <- app.checkInformation
      contactDetails <- checkInformation.contactDetails
    } yield contactDetails.telephoneNumber
  }
}
