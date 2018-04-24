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

package unit.utils

import java.util.UUID._

import model._
import org.joda.time.DateTime
import uk.gov.hmrc.time.DateTimeUtils

object ApplicationGenerator {

  def anApplicationResponse(dateTime: DateTime = DateTimeUtils.now): ApplicationResponse =
    new ApplicationResponse(randomUUID(), "appName", "deployedTo", None, Set.empty, dateTime, new Privileged(), new ApplicationState(),
      RateLimitTier.BRONZE, Some("termsUrl"), Some("privacyPolicyUrl"), None, None)

  def anApplicationResponseWith(approval: ApprovedApplication): ApplicationResponse = anApplicationResponse().copy(approvedDetails = Some(approval))

  def anApplicationResponseWith(checkInformation: CheckInformation): ApplicationResponse = anApplicationResponse().copy(checkInformation = Some(checkInformation))

  def anApprovedApplication(dateTime: DateTime = DateTimeUtils.now): ApprovedApplication =
    new ApprovedApplication(
      new ApplicationReviewDetails(id = "reviewId", name = "appName", description = "app description", rateLimitTier = None,
        submission = new SubmissionDetails("submitterName", "submitterEmail", dateTime), reviewContactName = Some("review contact name"),
        reviewContactEmail = Some("review contact email"), reviewContactTelephone = Some("review contact telephone"), applicationDetails = None),
      admins = Seq.empty,
      approvedBy = "approved by",
      approvedOn = dateTime,
      verified = true
    )

  def aCheckInformation(): CheckInformation =
    new CheckInformation(contactDetails = Some(new ContactDetails("contactFullName", "contactEmail", "contactTelephone")),
      confirmedName = true, providedPrivacyPolicyURL = true, providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details"))

}