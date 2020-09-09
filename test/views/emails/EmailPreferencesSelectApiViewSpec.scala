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
import model.{LoggedInUser, TopicOptionChoice}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import utils.ViewHelpers._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSelectApiView
import model.APIDefinition

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

    "show correct title and options when no filter provided" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectApiView.render(dropDownApis, Seq.empty, request, LoggedInUser(None), messagesProvider)
      val document: Document = Jsoup.parse(result.body)

      println(document)
      result.contentType must include("text/html")
      validatePageHeader(document, "Email users interested in a specific API")
      validateNonSelectedApiDropDown(document, dropDownApis, "Select an API")
      //TODO Validate dropdown exist and contains all options
      //validate form destination?
      //validate button 

  




    }
  }

  def simpleAPIDefinition(serviceName: String, name: String): APIDefinition =
   APIDefinition(serviceName, "url1", name, "desc", "context", Seq.empty, None, None)
  
}
