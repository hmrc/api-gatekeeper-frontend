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

package uk.gov.hmrc.gatekeeper.testdata

import java.time.Instant

import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{CheckInformation, ContactDetails, TermsOfUseAgreement}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

trait CheckInformationTestData {

  val defaultCheckInformation: CheckInformation =
    CheckInformation(
      contactDetails = Some(
        ContactDetails(
          fullname = FullName("Holly Golightly"),
          email = LaxEmailAddress("holly.golightly@example.com"),
          telephoneNumber = "020 1122 3344"
        )
      ),
      confirmedName = true,
      providedPrivacyPolicyURL = true,
      providedTermsAndConditionsURL = true,
      applicationDetails = Some(""),
      termsOfUseAgreements = List(
        TermsOfUseAgreement(
          emailAddress = LaxEmailAddress("test@example.com"),
          timeStamp = Instant.ofEpochSecond(1459868573962L),
          version = "1.0"
        )
      )
    )
}
