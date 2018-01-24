/*
 * Copyright 2018 HM Revenue & Customs
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
import model.Forms._
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec


class FormValidationSpec  extends UnitSpec with Matchers {

  "AccessOverrideForm" should {

    "fail validation with empty scopes" in {
      val invalidAccessOverrideForm = Map("grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "")
      val boundForm = accessOverridesForm.bind(invalidAccessOverrideForm)
      boundForm.errors.length shouldBe 1
    }

    "fail validation with invalid scope format" in {
      val invalidAccessOverrideForm = Map("grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "test1 \n test2")
      val boundForm = accessOverridesForm.bind(invalidAccessOverrideForm)
      boundForm.errors.length shouldBe 1
    }

    "pass validation with valid scopes" in {
      val validAccessOverrideForm = Map("grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "email, openid:mdtp, openid, openid:hmrc-enrolments, openid:government-gateway")
      val boundForm = accessOverridesForm.bind(validAccessOverrideForm)
      boundForm.errors shouldBe List.empty
    }
  }
}