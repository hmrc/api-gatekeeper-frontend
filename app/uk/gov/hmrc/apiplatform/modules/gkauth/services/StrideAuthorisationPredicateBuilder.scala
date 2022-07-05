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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import uk.gov.hmrc.apiplatform.modules.gkauth.config.StrideAuthRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

object StrideAuthorisationPredicateForGatekeeperRole {
  def apply(strideAuthRoles: StrideAuthRoles)(strideRoleRequired: GatekeeperStrideRole): Predicate = {
    import strideAuthRoles._
    
    val adminEnrolment = Enrolment(adminRole)
    lazy val superUserEnrolment = Enrolment(superUserRole)
    lazy val userEnrolment = Enrolment(userRole)

    strideRoleRequired match {
      case GatekeeperRoles.ADMIN => adminEnrolment
      case GatekeeperRoles.SUPERUSER => adminEnrolment or superUserEnrolment
      case GatekeeperRoles.USER => adminEnrolment or superUserEnrolment or userEnrolment
    }
  }
}
