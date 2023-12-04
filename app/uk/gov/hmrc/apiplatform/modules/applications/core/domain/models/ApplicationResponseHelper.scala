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

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

object ApplicationResponseHelper {

  implicit class AdminsSyntax(application: ApplicationResponse) {

    def isSoleAdmin(emailAddress: LaxEmailAddress): Boolean =
      application.admins.map(_.emailAddress).contains(emailAddress) && application.admins.size == 1
  }

  implicit class LocationsSyntax(application: ApplicationResponse) {

    lazy val privacyPolicyLocation: PrivacyPolicyLocation = application.access match {
      case Access.Standard(_, _, _, _, _, Some(ImportantSubmissionData(_, _, _, _, privacyPolicyLocation, _))) => privacyPolicyLocation
      case Access.Standard(_, _, Some(url), _, _, _)                                                           => PrivacyPolicyLocations.Url(url)
      case _                                                                                                   => PrivacyPolicyLocations.NoneProvided
    }

    lazy val termsAndConditionsLocation: TermsAndConditionsLocation = application.access match {
      case Access.Standard(_, _, _, _, _, Some(ImportantSubmissionData(_, _, _, termsAndConditionsLocation, _, _))) => termsAndConditionsLocation
      case Access.Standard(_, Some(url), _, _, _, _)                                                                => TermsAndConditionsLocations.Url(url)
      case _                                                                                                        => TermsAndConditionsLocations.NoneProvided
    }
  }
}
