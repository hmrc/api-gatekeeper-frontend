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

object StateHelper {

  implicit class StateSyntax(state: State) {

    val displayText: String = state match {
      case State.TESTING                                     => "Created"
      case State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION => "Pending Responsible Individual Verification"
      case State.PENDING_GATEKEEPER_APPROVAL                 => "Pending gatekeeper check"
      case State.PENDING_REQUESTER_VERIFICATION              => "Pending submitter verification"
      case State.PRE_PRODUCTION                              => "Active"
      case State.PRODUCTION                                  => "Active"
      case State.DELETED                                     => "Deleted"
    }

    val additionalInformation: String = state match {
      case State.TESTING                                     =>
        "A production application that its admin has created but not submitted for checking"
      case State.PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION =>
        "A production application that has been submitted for checking, but the responsible individual has not completed the email verification process"
      case State.PENDING_GATEKEEPER_APPROVAL                 =>
        "A production application that one of its admins has submitted for checking"
      case State.PENDING_REQUESTER_VERIFICATION              =>
        "A production application that has passed checking in Gatekeeper but the submitter has not completed the email verification process"
      case State.PRE_PRODUCTION                              =>
        "A production application that has passed checking, been verified, and is waiting for the user to confirm that they have carried out some initial setup"
      case State.PRODUCTION                                  =>
        "A production application that has passed checking, been verified and set up, and is therefore fully active - or any sandbox application"
      case State.DELETED                                     =>
        "An application that has been deleted and is no longer active"
    }
  }
}
