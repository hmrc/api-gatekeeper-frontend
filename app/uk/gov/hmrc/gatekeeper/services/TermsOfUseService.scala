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

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import javax.inject.Singleton

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{CheckInformation, GKApplicationResponse, TermsOfUseAgreement}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.TermsOfUseAcceptance
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails

object TermsOfUseService {
  case class TermsOfUseAgreementDisplayDetails(emailAddress: String, date: String, version: String)
}

@Singleton
class TermsOfUseService {

  def formatInstant(instant: Instant) = instant.atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("d MMMM yyyy"))

  private def getAgreementDetailsFromCheckInformation(checkInformation: CheckInformation): List[TermsOfUseAgreementDisplayDetails] = {
    checkInformation.termsOfUseAgreements.map((toua: TermsOfUseAgreement) => TermsOfUseAgreementDisplayDetails(toua.emailAddress.text, formatInstant(toua.timeStamp), toua.version))
  }

  private def getAgreementFromCheckInformation(application: GKApplicationResponse): Option[TermsOfUseAgreementDisplayDetails] = {
    application.checkInformation match {
      case Some(chkInfo) => getAgreementDetailsFromCheckInformation(chkInfo).lastOption
      case _             => None
    }
  }

  private def getAgreementDetailsFromStandardApp(std: Access.Standard): List[TermsOfUseAgreementDisplayDetails] = {
    std.importantSubmissionData.fold[List[TermsOfUseAgreementDisplayDetails]](List.empty)(isd =>
      isd.termsOfUseAcceptances
        .map((toua: TermsOfUseAcceptance) =>
          TermsOfUseAgreementDisplayDetails(toua.responsibleIndividual.emailAddress.text, formatInstant(toua.dateTime), "2")
        )
    )
  }

  private def getAgreementFromStandardApp(application: GKApplicationResponse): Option[TermsOfUseAgreementDisplayDetails] = {
    application.access match {
      case std: Access.Standard => getAgreementDetailsFromStandardApp(std).lastOption
      case _                    => None
    }
  }

  def getAgreementDetails(application: GKApplicationResponse): Option[TermsOfUseAgreementDisplayDetails] =
    getAgreementFromStandardApp(application).fold(getAgreementFromCheckInformation(application))(Some(_))
}
