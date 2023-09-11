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

package uk.gov.hmrc.gatekeeper.models.view

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.gatekeeper.models.applications.NewApplication
import uk.gov.hmrc.gatekeeper.models.{RegisteredUser, StateHistory}
import uk.gov.hmrc.gatekeeper.services.TermsOfUseService.TermsOfUseAgreementDisplayDetails

case class ResponsibleIndividualHistoryItem(name: String, email: String, fromDate: String, toDate: String)

case class ApplicationViewModel(
    developers: List[RegisteredUser],
    application: NewApplication,
    subscriptions: List[(String, List[(ApiVersionNbr, ApiStatus)])],
    subscriptionsThatHaveFieldDefns: List[(String, List[(ApiVersionNbr, ApiStatus)])],
    stateHistory: List[StateHistory],
    hasSubmissions: Boolean,
    gatekeeperApprovalsUrl: String,
    history: List[ResponsibleIndividualHistoryItem],
    maybeLatestTOUAgreement: Option[TermsOfUseAgreementDisplayDetails],
    isEligibleForTermsOfUseInvite: Boolean,
    termsOfUseInvitationUrl: String
  )
