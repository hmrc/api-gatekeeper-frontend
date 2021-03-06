/*
 * Copyright 2021 HM Revenue & Customs
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

package model.view

import model.applications.NewApplication
import model.ApiVersion
import model.StateHistory
import model.TermsOfUseAgreement
import model.ApiStatus.ApiStatus
import model.RegisteredUser

case class ApplicationViewModel(
  developers: List[RegisteredUser],
  application: NewApplication,
  subscriptions: List[(String, List[(ApiVersion, ApiStatus)])],
  subscriptionsThatHaveFieldDefns: List[(String, List[(ApiVersion, ApiStatus)])],
  stateHistory: List[StateHistory],
  isAtLeastSuperUser: Boolean,
  isAdmin: Boolean
) {
  val maybeLatestTOUAgreement: Option[TermsOfUseAgreement] = application.checkInformation.flatMap(_.latestTOUAgreement)
}
