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

package controllers

import model.Forms.UpdateGrantLengthForm
import play.api.data.FormError
import utils.AsyncHmrcSpec

class FormValidationSpec extends AsyncHmrcSpec {
  "HandleUpliftForm" should {
    val validHandleUpliftWithAcceptForm = Map("action" -> "APPROVE", "reason" -> "")
    val validHandleUpliftWithRejectForm = Map("action" -> "REJECT", "reason" -> "A similar name is already taken by another application")

    "validate a valid accept form" in {
      val boundForm = HandleUpliftForm.form.bind(validHandleUpliftWithAcceptForm)
      boundForm.errors shouldBe List()
      boundForm.globalErrors shouldBe List()
    }

    "validate a valid reject form" in {
      val boundForm = HandleUpliftForm.form.bind(validHandleUpliftWithAcceptForm)
      boundForm.errors shouldBe List()
      boundForm.globalErrors shouldBe List()
    }

    "validate invalid empty reject reason form" in {
      val boundForm = HandleUpliftForm.form.bind(validHandleUpliftWithRejectForm + ("reason" -> ""))
      boundForm.errors shouldBe List(FormError("reason", List("error.required")))
      boundForm.globalErrors shouldBe List()
    }

    "validate empty action form" in {
      val boundForm = HandleUpliftForm.form.bind(validHandleUpliftWithAcceptForm + ("action" -> ""))
      boundForm.errors shouldBe List(FormError("action", List("invalid.action")))
      boundForm.globalErrors shouldBe List()
    }

    "validate invalid action form" in {
      val boundForm = HandleUpliftForm.form.bind(validHandleUpliftWithAcceptForm + ("action" -> "INVALID"))
      boundForm.errors shouldBe List(FormError("action", List("invalid.action")))
      boundForm.globalErrors shouldBe List()
    }
  }

  "UpdateRateLimitForm" should {

    "validate a valid filled form" in {
      val boundForm = UpdateRateLimitForm.form.bind(Map("tier" -> "GOLD"))
      boundForm.errors shouldBe List()
      boundForm.globalErrors shouldBe List()
    }

    "validate a valid empty form" in {
      val boundForm = UpdateRateLimitForm.form.bind(Map("tier" -> ""))
      boundForm.errors shouldBe List(FormError("tier", List("error.required")))
      boundForm.globalErrors shouldBe List()
    }
  }

  "UpdateGrantLengthForm" should {

    "validate a properly filled form" in {
      val boundForm = UpdateGrantLengthForm.form.bind(Map("grantLength" -> "547"))
      boundForm.errors shouldBe List()
      boundForm.globalErrors shouldBe List()
    }

    "validate an empty form" in {
      val boundForm = UpdateGrantLengthForm.form.bind(Map("grantLength" -> ""))
      boundForm.errors shouldBe List(FormError("grantLength", List("grantLength.required")))
      boundForm.globalErrors shouldBe List()
    }
  }
}
