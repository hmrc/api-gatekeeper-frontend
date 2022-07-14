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

package uk.gov.hmrc.gatekeeper.modules.sms.views

import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.modules.sms.model.Forms.SendSmsForm
import uk.gov.hmrc.gatekeeper.modules.sms.views.html.SendSmsView
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport.CSRFFakeRequest
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers.elementExistsById
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec

class SendSmsViewSpec extends CommonViewSpec {

  "Send Sms View" should {
    implicit val request = FakeRequest().withCSRFToken
    implicit val userName = LoggedInUser(Some("gate.keeper"))
    implicit val messages = app.injector.instanceOf[MessagesControllerComponents].messagesApi.preferred(request)

    val sendSmsView = app.injector.instanceOf[SendSmsView]

    "show the Send SMS button" in {

      val document = Jsoup.parse(sendSmsView(SendSmsForm.form).body)

      document.getElementById("heading").text() shouldBe "Send SMS"
      document.getElementById("phoneNumber-label").text() shouldBe "Enter the Phone number"
      elementExistsById(document, "phoneNumber") shouldBe true
      elementExistsById(document, "submit") shouldBe true
      document.getElementById("submit").text() shouldBe "Send SMS"

    }
  }

}
