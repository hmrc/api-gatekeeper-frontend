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

import play.api.libs.json.Json

// TODO - Remove Enumeration
object State extends Enumeration {
  type State = Value
  val TESTING, PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION, PENDING_GATEKEEPER_APPROVAL, PENDING_REQUESTER_VERIFICATION, PRE_PRODUCTION, PRODUCTION, DELETED = Value
  implicit val format                                                                                                                                        = Json.formatEnum(State)

  val displayedState: State => String = {
    case TESTING                                     => "Created"
    case PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION => "Pending Responsible Individual Verification"
    case PENDING_GATEKEEPER_APPROVAL                 => "Pending gatekeeper check"
    case PENDING_REQUESTER_VERIFICATION              => "Pending submitter verification"
    case PRE_PRODUCTION                              => "Active"
    case PRODUCTION                                  => "Active"
    case DELETED                                     => "Deleted"
  }

  val additionalInformation: State => String = {
    case TESTING                                     =>
      "A production application that its admin has created but not submitted for checking"
    case PENDING_RESPONSIBLE_INDIVIDUAL_VERIFICATION =>
      "A production application that has been submitted for checking, but the responsible individual has not completed the email verification process"
    case PENDING_GATEKEEPER_APPROVAL                 =>
      "A production application that one of its admins has submitted for checking"
    case PENDING_REQUESTER_VERIFICATION              =>
      "A production application that has passed checking in Gatekeeper but the submitter has not completed the email verification process"
    case PRE_PRODUCTION                              =>
      "A production application that has passed checking, been verified, and is waiting for the user to confirm that they have carried out some initial setup"
    case PRODUCTION                                  =>
      "A production application that has passed checking, been verified and set up, and is therefore fully active - or any sandbox application"
    case DELETED                                     =>
      "An application that has been deleted and is no longer active"
  }

  implicit class StateHelpers(state: State) {
    def isApproved                     = state == State.PRE_PRODUCTION || state == State.PRODUCTION
    def isPendingGatekeeperApproval    = state == State.PENDING_GATEKEEPER_APPROVAL
    def isPendingRequesterVerification = state == State.PENDING_REQUESTER_VERIFICATION
    def isDeleted                      = state == State.DELETED
  }
}
