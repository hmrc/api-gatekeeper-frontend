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
import model.{APIDefinition, LoggedInUser}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSelectApiView

class EmailPreferencesSelectApiViewSpec extends CommonViewSpec with UserTableHelper with EmailUsersHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSelectApiView: EmailPreferencesSelectApiView = app.injector.instanceOf[EmailPreferencesSelectApiView]
  }

  "email preferences specific api view" must {

     val api1 = simpleAPIDefinition(serviceName="serviceName1", name="api1")
     val api2 = simpleAPIDefinition(serviceName="serviceName2", name="api2")
     val api3 = simpleAPIDefinition(serviceName="serviceName3", name="api3")
     val dropDownApis = Seq(api1, api2, api3)


    def validateStaticPageElements(document: Document){
      validatePageHeader(document, "Email users interested in a specific API")
      validateNonSelectedApiDropDown(document, dropDownApis, "Select an API")

      validateFormDestination(document, "apiSelectionForm", "/api-gatekeeper/emails/email-preferences/by-specific-api")
      validateButtonText(document, "submit", "Select API")
    }

    "show correct title and options when no selectedAPis provided" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectApiView.render(dropDownApis, Seq.empty, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)
      validateStaticPageElements(document)
    }
    "show correct title and options when selectedAPis are provided" in new Setup {
      val selectedApis = Seq(api2)
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectApiView.render(dropDownApis, selectedApis, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)
      validateStaticPageElements(document)
        // As above, check hidden fields
      validateHiddenSelectedApiValues(document, selectedApis)  

    }


  }

  def simpleAPIDefinition(serviceName: String, name: String): APIDefinition =
   APIDefinition(serviceName, "url1", name, "desc", "context", Seq.empty, None, None)
  
}
