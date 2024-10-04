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

  implicit class AdminsSyntax(application: ApplicationWithCollaborators) {

    def isSoleAdmin(emailAddress: LaxEmailAddress): Boolean =
      application.admins.map(_.emailAddress).contains(emailAddress) && application.admins.size == 1
  }

  implicit class LocationsSyntax(application: ApplicationWithCollaborators) {

    lazy val privacyPolicyLocation: PrivacyPolicyLocation = application.access match {
      case s: Access.Standard => s.privacyPolicyLocation.getOrElse(PrivacyPolicyLocations.NoneProvided)
      case _                  => PrivacyPolicyLocations.NoneProvided
    }

    lazy val termsAndConditionsLocation: TermsAndConditionsLocation = application.access match {
      case s: Access.Standard => s.termsAndConditionsLocation.getOrElse(TermsAndConditionsLocations.NoneProvided)
      case _                  => TermsAndConditionsLocations.NoneProvided
    }
  }
}
