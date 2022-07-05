/*
 * Copyright 2022 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.models.applications.NewApplication
import uk.gov.hmrc.gatekeeper.models.ApiVersion
import uk.gov.hmrc.gatekeeper.models.StateHistory
import uk.gov.hmrc.gatekeeper.models.TermsOfUseAgreement
import uk.gov.hmrc.gatekeeper.models.ApiStatus.ApiStatus
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRole

case class ApplicationViewModel(
  developers: List[RegisteredUser],
  application: NewApplication,
  subscriptions: List[(String, List[(ApiVersion, ApiStatus)])],
  subscriptionsThatHaveFieldDefns: List[(String, List[(ApiVersion, ApiStatus)])],
  stateHistory: List[StateHistory],
  hasSubmissions: Boolean,
  gatekeeperApprovalsUrl: String
) {
  val maybeLatestTOUAgreement: Option[TermsOfUseAgreement] = application.checkInformation.flatMap(_.latestTOUAgreement)
}