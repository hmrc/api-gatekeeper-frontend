/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.model

import java.util.UUID

import model._
import org.joda.time.DateTime
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import AccessType._

class ModelSpec  extends UnitSpec with Matchers {

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

  "RateLimitTier" should {

    "have all rate limit tiers" in {
      import RateLimitTier._
      RateLimitTier.values shouldBe Set(PLATINUM, GOLD, SILVER, BRONZE)
    }

    "convert string value to enum with lowercase" in {
      RateLimitTier.from("gold") shouldBe Some(RateLimitTier.GOLD)
      RateLimitTier.from("bronze") shouldBe Some(RateLimitTier.BRONZE)
    }

    "convert string value to enum with mixedcase" in {
      RateLimitTier.from("gOld") shouldBe Some(RateLimitTier.GOLD)
      RateLimitTier.from("SilVeR") shouldBe Some(RateLimitTier.SILVER)
    }

    "convert string value to None when undefined or empty" in {
      RateLimitTier.from("unknown") shouldBe None
      RateLimitTier.from("") shouldBe None
    }
  }

  "Application.isSoleAdmin" should {
    val emailAddress = "admin@example.com"
    val admin = Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR)
    val developer = Collaborator(emailAddress, CollaboratorRole.DEVELOPER)
    val otherAdmin = Collaborator("otheradmin@example.com", CollaboratorRole.ADMINISTRATOR)
    val otherDeveloper = Collaborator("someone@example.com", CollaboratorRole.DEVELOPER)

    def application(teamMembers: Set[Collaborator]) =
      ApplicationResponse(
        UUID.randomUUID(), "clientid", "gatewayId", "application", "PRODUCTION", None, teamMembers, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

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
      AccessType.values shouldBe Set(STANDARD, PRIVILEGED, ROPC)
    }

    "convert strings with any case to AccessType" in {
      AccessType.from("standard") shouldBe Some(STANDARD)
      AccessType.from("Standard") shouldBe Some(STANDARD)

      AccessType.from("privileged") shouldBe Some(PRIVILEGED)
      AccessType.from("priVILeged") shouldBe Some(PRIVILEGED)

      AccessType.from("ropc") shouldBe Some(ROPC)
      AccessType.from("ROPC") shouldBe Some(ROPC)
    }

    "convert unknown strings to None" in {
      AccessType.from("anything") shouldBe None
      AccessType.from("") shouldBe None
    }
  }

}
