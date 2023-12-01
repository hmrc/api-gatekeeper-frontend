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

package uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models

case class ImportantSubmissionData(
    organisationUrl: Option[String] = None,
    responsibleIndividual: ResponsibleIndividual,
    serverLocations: Set[ServerLocation],
    termsAndConditionsLocation: TermsAndConditionsLocation,
    privacyPolicyLocation: PrivacyPolicyLocation,
    termsOfUseAcceptances: List[TermsOfUseAcceptance]
  )

object ImportantSubmissionData {
  import play.api.libs.json._
  implicit val format: OFormat[ImportantSubmissionData] = Json.format[ImportantSubmissionData]
}
