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

package uk.gov.hmrc.apiplatform.modules.sms.views

import modules.sms.views.html.SendSmsSuccessView
import org.jsoup.Jsoup
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec

class SendSmsSuccessViewSpec extends CommonViewSpec {

  "Send Sms Success View" should {
    implicit val request = FakeRequest().withCSRFToken
    implicit val userName = LoggedInUser(Some("gate.keeper"))
    implicit val messages = app.injector.instanceOf[MessagesControllerComponents].messagesApi.preferred(request)

    val sendSmsView = app.injector.instanceOf[SendSmsSuccessView]

    "show the Send SMS success message" in {
      val message = "SMS successfully sent"

      val document = Jsoup.parse(sendSmsView(message).body)

      document.getElementById("heading").text() shouldBe "Send SMS Success"
      document.getElementById("message").text() shouldBe message

    }
  }

}
