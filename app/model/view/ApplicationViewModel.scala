/*
 * Copyright 2020 HM Revenue & Customs
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

import model.User
import model.applications.NewApplication
import model.ApiVersion
import model.StateHistory
import model.TermsOfUseAgreement
import model.APIStatus.APIStatus

case class ApplicationViewModel(
  developers: List[User],
  application: NewApplication,
  subscriptions: Seq[(String, Seq[(ApiVersion, APIStatus)])],
  subscriptionsThatHaveFieldDefns: Seq[(String, Seq[(ApiVersion, APIStatus)])],
  stateHistory: Seq[StateHistory],
  isAtLeastSuperUser: Boolean,
  isAdmin: Boolean,
  maybeLatestTOUAgreement: Option[TermsOfUseAgreement]
)
