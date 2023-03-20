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

package uk.gov.hmrc.gatekeeper.services

import java.time.{LocalDateTime, ZoneOffset}
import org.joda.time.DateTime
import java.time.format.DateTimeFormatter
import org.joda.time.format.DateTimeFormat

import uk.gov.hmrc.gatekeeper.models.{CheckInformation, ImportantSubmissionData, Privileged, ResponsibleIndividual, Standard, TermsOfUseAcceptance, TermsOfUseAgreement, TermsAndConditionsLocation, PrivacyPolicyLocation}
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models.PrivacyPolicyLocation

class TermsOfUseServiceSpec extends AsyncHmrcSpec with ApplicationBuilder {


  val timestamp                  = LocalDateTime.now(ZoneOffset.UTC)
  val dateTime                   = DateTime.now
  val email1_2                    = "bob1.2@example.com"
  val email2                     = "bob2@example.com"
  val name                       = "Bob Example"
  val responsibleIndividual      = ResponsibleIndividual(ResponsibleIndividual.Name(name), ResponsibleIndividual.EmailAddress(email2))
  val version1_2                 = "1.2"
  val version2                   = "2"
  val appWithNoAgreements        = buildApplication()
  val checkInfoAgreement         = TermsOfUseAgreement(email1_2, dateTime, version1_2)
  val checkInformation           = CheckInformation(termsOfUseAgreements = List(checkInfoAgreement))
  val stdAppAgreement            = TermsOfUseAcceptance(responsibleIndividual, timestamp)
  val importantSubmissionData    = ImportantSubmissionData(TermsAndConditionsLocation.InDesktopSoftware, PrivacyPolicyLocation.InDesktopSoftware, List(stdAppAgreement))
  val appWithCheckInfoAgreements = buildApplication(checkInformation = Some(checkInformation))
  val appWithStdAppAgreements    = appWithNoAgreements.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData)))
  val nonStdApp                  = appWithNoAgreements.copy(access = Privileged())
  val underTest                  = new TermsOfUseService()

  def formatDateTime(localDateTime: LocalDateTime) = localDateTime.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
  def formatJodaDateTime(dateTime: DateTime) = DateTimeFormat.forPattern("d MMMM yyyy").print(dateTime)

  "getAgreementDetails" should {
    "return None if no agreements found" in {
      val maybeAgreement = underTest.getAgreementDetails(appWithNoAgreements)
      maybeAgreement shouldBe None
    }
    "return correctly populated agreement if details found in CheckInformation" in {
      val maybeAgreement = underTest.getAgreementDetails(appWithCheckInfoAgreements)
      maybeAgreement shouldBe Some(TermsOfUseAgreementDisplayDetails(email1_2, formatJodaDateTime(dateTime), version1_2))
    }
    "return correctly populated agreement if details found in ImportantSubmissionData" in {
      val maybeAgreement = underTest.getAgreementDetails(appWithStdAppAgreements)
      maybeAgreement shouldBe Some(TermsOfUseAgreementDisplayDetails(email2, formatDateTime(timestamp), version2))
    }
    "return correctly populated agreement if details found in ImportantSubmissionData AND in CheckInformation" in {
      val maybeAgreement = underTest.getAgreementDetails(appWithCheckInfoAgreements.copy(access = Standard(importantSubmissionData = Some(importantSubmissionData))))
      maybeAgreement shouldBe Some(TermsOfUseAgreementDisplayDetails(email2, formatDateTime(timestamp), version2))
    }
    "return None if non-standard app is checked" in {
      val maybeAgreement = underTest.getAgreementDetails(nonStdApp)
      maybeAgreement shouldBe None
    }
    "return None if ImportantSubmissionData is missing" in {
      val maybeAgreement =
        underTest.getAgreementDetails(appWithStdAppAgreements.copy(access = appWithStdAppAgreements.access.asInstanceOf[Standard].copy(importantSubmissionData = None)))
      maybeAgreement shouldBe None
    }
    "return None if ImportantSubmissionData.termsOfUseAcceptances is empty" in {
      val importantSubmissionData = appWithStdAppAgreements.access.asInstanceOf[Standard].importantSubmissionData.get
      val maybeAgreement          = underTest.getAgreementDetails(appWithStdAppAgreements.copy(access =
        appWithStdAppAgreements.access.asInstanceOf[Standard].copy(importantSubmissionData = Some(importantSubmissionData.copy(termsOfUseAcceptances = List.empty)))
      ))
      maybeAgreement shouldBe None
    }
  }
}
