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

package utils

import java.util.UUID._

import model.State._
import model._
import org.joda.time.DateTime
import uk.gov.hmrc.time.DateTimeUtils

object ApplicationGenerator {

  def anApplicationWithHistory(applicationResponse: ApplicationResponse = anApplicationResponse(),
                               stateHistories: Seq[StateHistory] = Seq.empty): ApplicationWithHistory = {
    ApplicationWithHistory(applicationResponse, stateHistories)
  }

  def anApplicationResponse(createdOn: DateTime = DateTimeUtils.now, lastAccess: DateTime = DateTimeUtils.now): ApplicationResponse = {
    ApplicationResponse(randomUUID(), "clientid", "gatewayId", "appName", "deployedTo", None, Set.empty, createdOn,
      lastAccess, Privileged(), ApplicationState(), RateLimitTier.BRONZE, Some("termsUrl"), Some("privacyPolicyUrl"), None)
  }

  def anApplicationResponseWith(checkInformation: CheckInformation): ApplicationResponse = {
    anApplicationResponse().copy(checkInformation = Some(checkInformation))
  }

  def aCheckInformation(): CheckInformation = {
    CheckInformation(contactDetails = Some(ContactDetails("contactFullName", "contactEmail", "contactTelephone")),
      confirmedName = true, providedPrivacyPolicyURL = true, providedTermsAndConditionsURL = true,
      applicationDetails = Some("application details"))
  }

  def aStateHistory(state: State, changedAt: DateTime = DateTimeUtils.now): StateHistory = {
    StateHistory(randomUUID(), state, anActor(), None, changedAt)
  }

  def anActor() = Actor("actor id")
}