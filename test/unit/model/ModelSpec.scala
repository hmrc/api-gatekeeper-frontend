/*
 * Copyright 2017 HM Revenue & Customs
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

import model.RateLimitTier
import model.UpliftAction
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec


class ModelSpec  extends UnitSpec with Matchers {

  "UpliftAction" should {
    "convert string value to enum with lowercase" in {
      UpliftAction.from("approve") shouldBe Some(UpliftAction.APPROVE)
      UpliftAction.from("reject") shouldBe Some(UpliftAction.REJECT)
    }

    "convert string value to enum with mixedcase" in {
      UpliftAction.from("aPProve") shouldBe Some(UpliftAction.APPROVE)
      UpliftAction.from("rEJect") shouldBe Some(UpliftAction.REJECT)
    }

    "convert string value to None when undefined or empty" in {
      UpliftAction.from("unknown") shouldBe None
      UpliftAction.from("") shouldBe None
    }
  }

  "RateLimitTier" should {
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

}
