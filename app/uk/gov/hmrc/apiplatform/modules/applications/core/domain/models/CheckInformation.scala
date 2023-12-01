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

case class CheckInformation(
    contactDetails: Option[ContactDetails] = None,
    confirmedName: Boolean = false,
    apiSubscriptionsConfirmed: Boolean = false,
    apiSubscriptionConfigurationsConfirmed: Boolean = false,
    providedPrivacyPolicyURL: Boolean = false,
    providedTermsAndConditionsURL: Boolean = false,
    applicationDetails: Option[String] = None,
    teamConfirmed: Boolean = false,
    termsOfUseAgreements: List[TermsOfUseAgreement] = List.empty
  )

object CheckInformation {
  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  private val reads: Reads[CheckInformation] = (
    (JsPath \ "contactDetails").readNullable[ContactDetails] and
      (JsPath \ "confirmedName").read[Boolean] and
      ((JsPath \ "apiSubscriptionsConfirmed").read[Boolean] or Reads.pure(false)) and
      ((JsPath \ "apiSubscriptionConfigurationsConfirmed").read[Boolean] or Reads.pure(false)) and
      (JsPath \ "providedPrivacyPolicyURL").read[Boolean] and
      (JsPath \ "providedTermsAndConditionsURL").read[Boolean] and
      (JsPath \ "applicationDetails").readNullable[String] and
      ((JsPath \ "teamConfirmed").read[Boolean] or Reads.pure(false)) and
      ((JsPath \ "termsOfUseAgreements").read[List[TermsOfUseAgreement]] or Reads.pure(List.empty[TermsOfUseAgreement]))
  )(CheckInformation.apply _)

  implicit val format: Format[CheckInformation] = Format(reads, Json.writes[CheckInformation])
}
