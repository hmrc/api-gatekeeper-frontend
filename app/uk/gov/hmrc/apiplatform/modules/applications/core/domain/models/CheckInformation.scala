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

import java.time.LocalDateTime

import play.api.libs.json.Json

import uk.gov.hmrc.gatekeeper.models.ContactDetails

case class CheckInformation(
                             contactDetails: Option[ContactDetails] = None,
                             confirmedName: Boolean = false,
                             providedPrivacyPolicyURL: Boolean = false,
                             providedTermsAndConditionsURL: Boolean = false,
                             applicationDetails: Option[String] = None,
                             termsOfUseAgreements: List[TermsOfUseAgreement] = List.empty
                           ) {

  def latestTOUAgreement: Option[TermsOfUseAgreement] = {
    implicit val dateTimeOrdering: Ordering[LocalDateTime] = Ordering.fromLessThan(_ isBefore _)

    termsOfUseAgreements match {
      case Nil        => None
      case agreements => Option(agreements.maxBy(_.timeStamp))
    }
  }
}

object CheckInformation {

  implicit val formatTermsOfUseAgreement = Json.format[TermsOfUseAgreement]
  implicit val formatApprovalInformation = Json.format[CheckInformation]
}