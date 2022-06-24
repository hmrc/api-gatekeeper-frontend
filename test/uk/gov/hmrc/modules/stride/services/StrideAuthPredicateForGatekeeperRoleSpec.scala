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

package uk.gov.hmrc.modules.stride.services

import utils.AsyncHmrcSpec
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles
import uk.gov.hmrc.modules.stride.config.StrideAuthRoles
import uk.gov.hmrc.auth.core.Enrolment

class StrideAuthPredicateForGatekeeperRoleSpec extends AsyncHmrcSpec {
  val roles = StrideAuthRoles("admin","super","user")
  
  import roles._

  "StrideAuthPredicateForGatekeeperRole" should {
    "contain admin role only when looking for GK.ADMIN" in {
      val predicate = StrideAuthPredicateForGatekeeperRole(roles)(GatekeeperRoles.ADMIN)

      predicate shouldBe Enrolment(adminRole)
    }

    "contain admin and super user roles when looking for GK.SUPERUSER" in {
      val predicate = StrideAuthPredicateForGatekeeperRole(roles)(GatekeeperRoles.SUPERUSER)

      predicate shouldBe (Enrolment(adminRole) or Enrolment(superUserRole))
    }

    "contain admin, super user and user roles when looking for GK.USER" in {
      val predicate = StrideAuthPredicateForGatekeeperRole(roles)(GatekeeperRoles.USER)

      predicate shouldBe (Enrolment(adminRole) or Enrolment(superUserRole) or Enrolment(userRole))
    }
  }
}