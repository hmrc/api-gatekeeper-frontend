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

package uk.gov.hmrc.gatekeeper.testdata

import org.joda.time._

import uk.gov.hmrc.gatekeeper.models.{ApplicationState, State}

trait ApplicationStateTestData extends CommonTestData {

  val productionState: ApplicationState = ApplicationState(
    name = State.PRODUCTION,
    requestedByEmailAddress = Some(administratorEmail.text),
    verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
    updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )

  val pendingApprovalState: ApplicationState = ApplicationState(
    name = State.PENDING_GATEKEEPER_APPROVAL,
    requestedByEmailAddress = Some(administratorEmail.text),
    verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
    updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )

  val stateForFetchAppResponseByEmail: ApplicationState = ApplicationState(
    name = State.PRODUCTION,
    requestedByEmailAddress = Some(developerEmail.text),
    verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
    updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )
}
