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

package uk.gov.hmrc.gatekeeper.models

import java.time.LocalDateTime

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Access, AccessType}
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponseHelper._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationState
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{Collaborator, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder

class ModelSpec extends AsyncHmrcSpec with ApplicationBuilder {

  "UpliftAction" should {
    "convert string value to enum with lowercase" in {
      UpliftAction.from("approve") shouldBe Some(UpliftAction.APPROVE)
      UpliftAction.from("reject") shouldBe Some(UpliftAction.REJECT)
    }

    "convert string value to enum with mixed case" in {
      UpliftAction.from("aPProve") shouldBe Some(UpliftAction.APPROVE)
      UpliftAction.from("rEJect") shouldBe Some(UpliftAction.REJECT)
    }

    "convert string value to None when undefined or empty" in {
      UpliftAction.from("unknown") shouldBe None
      UpliftAction.from("") shouldBe None
    }
  }

  "ApplicationHelper.isSoleAdmin" should {
    val emailAddress                                = "admin@example.com".toLaxEmail
    val admin                                       = Collaborators.Administrator(UserId.random, emailAddress)
    val developer                                   = Collaborators.Developer(UserId.random, emailAddress)
    val otherAdmin                                  = Collaborators.Administrator(UserId.random, "otheradmin@example.com".toLaxEmail)
    val otherDeveloper                              = Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)
    def application(teamMembers: Set[Collaborator]) =
      buildApplication(
        ApplicationId.random,
        ClientId("clientid"),
        "gatewayId",
        Some("application"),
        Environment.PRODUCTION,
        None,
        teamMembers,
        LocalDateTime.now(),
        Some(LocalDateTime.now()),
        access = Access.Standard(),
        state = ApplicationState()
      )

    "return true when the given email address is the only admin and no other team members" in {
      val app = application(Set(admin))
      app.isSoleAdmin(emailAddress) shouldBe true
    }

    "return true when the given email address is the only admin and other team members exist" in {
      val app = application(Set(admin, otherDeveloper))
      app.isSoleAdmin(emailAddress) shouldBe true
    }

    "return false when the given email address is not the only admin" in {
      val app = application(Set(admin, otherAdmin))
      app.isSoleAdmin(emailAddress) shouldBe false
    }

    "return false when the given email address is not an admin" in {
      val app = application(Set(developer, otherAdmin))
      app.isSoleAdmin(emailAddress) shouldBe false
    }
  }

  "AccessType" should {

    "contain all access types" in {
      AccessType.values shouldBe List(AccessType.STANDARD, AccessType.PRIVILEGED, AccessType.ROPC)
    }

    "convert strings with any case to AccessType" in {
      AccessType.apply("standard") shouldBe Some(AccessType.STANDARD)
      AccessType.apply("Standard") shouldBe Some(AccessType.STANDARD)

      AccessType.apply("privileged") shouldBe Some(AccessType.PRIVILEGED)
      AccessType.apply("priVILeged") shouldBe Some(AccessType.PRIVILEGED)

      AccessType.apply("ropc") shouldBe Some(AccessType.ROPC)
      AccessType.apply("ROPC") shouldBe Some(AccessType.ROPC)
    }

    "convert unknown strings to None" in {
      AccessType.apply("anything") shouldBe None
      AccessType.apply("") shouldBe None
    }
  }

}
