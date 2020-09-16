/*
 * Copyright 2020 HM Revenue & Customs
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

package views.emails

import mocks.config.AppConfigMock
import model.LoggedInUser
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSelectApiView

class EmailPreferencesSelectApiViewSpec extends CommonViewSpec with EmailPreferencesSelectAPIViewHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSelectApiView: EmailPreferencesSelectApiView = app.injector.instanceOf[EmailPreferencesSelectApiView]
  }

  "email preferences specific api view" must {

     val api1 = simpleAPIDefinition(serviceName="serviceName1", name="api1","context",  None, "1")
     val api2 = simpleAPIDefinition(serviceName="serviceName2", name="api2","context",  None, "1")
     val api3 = simpleAPIDefinition(serviceName="serviceName3", name="api3", "context",  None, "1")
     val dropDownApis = Seq(api1, api2, api3)

    "show correct title and options when no selectedAPis provided" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesSelectApiView.render(dropDownApis, Seq.empty, request, LoggedInUser(None), messagesProvider)
      
      validateSelectAPIPageWithNonePreviouslySelected(Jsoup.parse(result.body), dropDownApis)
    }
    
    "show correct title and options when selectedAPis are provided" in new Setup {
      val selectedApis = Seq(api2)
      val result: HtmlFormat.Appendable =
      emailPreferencesSelectApiView.render(dropDownApis, selectedApis, request, LoggedInUser(None), messagesProvider)

      validateSelectAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), dropDownApis, selectedApis)
    }
  }
}
