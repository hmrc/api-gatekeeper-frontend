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

  def getSubmittedBy(application: ApplicationResponse): Option[String] = {
    for {
      approvedDetails <- application.approvedDetails
      email <- Some(approvedDetails.details.submission.submitterEmail)
    } yield email
  }

  def getSubmittedOn(application: ApplicationResponse): Option[String] = {
    for {
      approvedDetails <- application.approvedDetails
      approvedOn <- Some(dateFormatter.print(approvedDetails.details.submission.submittedOn))
    } yield approvedOn
  }
}

object ApplicationReview {
  val dateFormatter = DateTimeFormat.forPattern("dd MMMM yyyy")

  def isApproved(app: ApplicationResponse): Boolean = app.approvedDetails.isDefined

  def getApprovedOn(app: ApplicationResponse): Option[String] = app.approvedDetails.map(approvedDetails =>
    dateFormatter.print(approvedDetails.approvedOn))

  def getApprovedBy(app: ApplicationResponse): Option[String] = app.approvedDetails.map(approvedDetails => approvedDetails.approvedBy)

  def getReviewContactName(app: ApplicationResponse): Option[String] = {
    for {
      approvedDetails <- app.approvedDetails
      name <- approvedDetails.details.reviewContactName
    } yield name
  }

  def getReviewContactEmail(app: ApplicationResponse): Option[String] = {
    for {
      approvedDetails <- app.approvedDetails
      email <- approvedDetails.details.reviewContactEmail
    } yield email
  }

  def getReviewContactTelephone(app: ApplicationResponse): Option[String] = {
    for {
      approvedDetails <- app.approvedDetails
      telephone <- approvedDetails.details.reviewContactTelephone
    } yield telephone
  }
}