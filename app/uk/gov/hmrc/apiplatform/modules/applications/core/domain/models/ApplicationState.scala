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

import java.time.{LocalDateTime, ZoneOffset}

case class ApplicationState(
    name: State = State.TESTING,
    requestedByEmailAddress: Option[String] = None,
    verificationCode: Option[String] = None,
    updatedOn: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC)
  ) {
  def isApproved                     = name.isApproved
  def isPendingGatekeeperApproval    = name.isPendingGatekeeperApproval
  def isPendingRequesterVerification = name.isPendingRequesterVerification
  def isDeleted                      = name.isDeleted
}
