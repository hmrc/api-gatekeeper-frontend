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

package uk.gov.hmrc.gatekeeper.models.applications

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{GrantLength, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.{ImportantSubmissionData, PrivacyPolicyLocation, TermsAndConditionsLocation}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._

class NewApplicationSpec extends HmrcSpec {
  val url            = "http://example.com"
  val standardAccess = Standard()

  val importantSubmissionData = ImportantSubmissionData(
    TermsAndConditionsLocation.NoneProvided,
    PrivacyPolicyLocation.NoneProvided,
    List.empty
  )

  val baseApplication = NewApplication(
    ApplicationId.random,
    ClientId.random,
    "gatewayId",
    "name",
    LocalDateTime.now(),
    Some(LocalDateTime.now()),
    None,
    Environment.PRODUCTION,
    access = standardAccess,
    rateLimitTier = RateLimitTier.BRONZE,
    blocked = false,
    grantLength = GrantLength.ONE_DAY.days
  )

  "privacy policy location" should {
    "be correct for old journey app when no location supplied" in {
      val application = baseApplication.copy(access = Standard(privacyPolicyUrl = None))
      application.privacyPolicyLocation shouldBe PrivacyPolicyLocation.NoneProvided
    }
    "be correct for old journey app when location was supplied" in {
      val application = baseApplication.copy(access = Standard(privacyPolicyUrl = Some(url)))
      application.privacyPolicyLocation shouldBe PrivacyPolicyLocation.Url(url)
    }
    "be correct for new journey app when location was url" in {
      val application = baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(privacyPolicyLocation = PrivacyPolicyLocation.Url(url)))))
      application.privacyPolicyLocation shouldBe PrivacyPolicyLocation.Url(url)
    }
    "be correct for new journey app when location was in desktop app" in {
      val application =
        baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(privacyPolicyLocation = PrivacyPolicyLocation.InDesktopSoftware))))
      application.privacyPolicyLocation shouldBe PrivacyPolicyLocation.InDesktopSoftware
    }
    "be correct for new journey app when location was not supplied" in {
      val application =
        baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(privacyPolicyLocation = PrivacyPolicyLocation.NoneProvided))))
      application.privacyPolicyLocation shouldBe PrivacyPolicyLocation.NoneProvided
    }
  }

  "terms and conditions location" should {
    "be correct for old journey app when no location supplied" in {
      val application = baseApplication.copy(access = Standard(termsAndConditionsUrl = None))
      application.termsAndConditionsLocation shouldBe TermsAndConditionsLocation.NoneProvided
    }
    "be correct for old journey app when location was supplied" in {
      val application = baseApplication.copy(access = Standard(termsAndConditionsUrl = Some(url)))
      application.termsAndConditionsLocation shouldBe TermsAndConditionsLocation.Url(url)
    }
    "be correct for new journey app when location was url" in {
      val application =
        baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(termsAndConditionsLocation = TermsAndConditionsLocation.Url(url)))))
      application.termsAndConditionsLocation shouldBe TermsAndConditionsLocation.Url(url)
    }
    "be correct for new journey app when location was in desktop app" in {
      val application =
        baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(termsAndConditionsLocation = TermsAndConditionsLocation.InDesktopSoftware))))
      application.termsAndConditionsLocation shouldBe TermsAndConditionsLocation.InDesktopSoftware
    }
    "be correct for new journey app when location was not supplied" in {
      val application =
        baseApplication.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData.copy(termsAndConditionsLocation = TermsAndConditionsLocation.NoneProvided))))
      application.termsAndConditionsLocation shouldBe TermsAndConditionsLocation.NoneProvided
    }
  }

}
