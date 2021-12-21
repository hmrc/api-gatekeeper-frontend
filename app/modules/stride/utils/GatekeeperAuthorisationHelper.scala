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

package modules.stride.utils

import uk.gov.hmrc.modules.stride.controllers.models.LoggedInRequest
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser

trait GatekeeperAuthorisationHelper {
  implicit def loggedIn(implicit request: LoggedInRequest[_]): LoggedInUser = LoggedInUser(request.name)
  
  def isAtLeastSuperUser(implicit request: LoggedInRequest[_], strideAuthConfig: StrideAuthConfig): Boolean = {
    request.authorisedEnrolments.getEnrolment(strideAuthConfig.superUserRole).isDefined || request.authorisedEnrolments.getEnrolment(strideAuthConfig.adminRole).isDefined
  }

  def isAdmin(implicit request: LoggedInRequest[_], strideAuthConfig: StrideAuthConfig): Boolean = {
    request.authorisedEnrolments.getEnrolment(strideAuthConfig.adminRole).isDefined
  }
}