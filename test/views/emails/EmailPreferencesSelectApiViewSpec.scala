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

package views.emails

import mocks.config.AppConfigMock
import model.APIAccessType.PUBLIC
import uk.gov.hmrc.modules.gkauth.domain.models.LoggedInUser
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesSelectApiView
import model.{ApiType, CombinedApi, CombinedApiCategory}

class EmailPreferencesSelectApiViewSpec extends CommonViewSpec with EmailPreferencesSelectAPIViewHelper{

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSelectApiView: EmailPreferencesSelectApiView = app.injector.instanceOf[EmailPreferencesSelectApiView]
  }

  "email preferences specific api view" must {

     val api1: CombinedApi = simpleAPI(serviceName="serviceName0", displayName="displayName0",  List.empty,  ApiType.REST_API, Some(PUBLIC))
     val api2 = simpleAPI(serviceName="serviceName1", displayName="displayName1",  List.empty,  ApiType.REST_API, Some(PUBLIC))
     val api3 = simpleAPI(serviceName="serviceName2", displayName="displayName2",  List.empty,  ApiType.XML_API, Some(PUBLIC))
     val dropDownApis = Seq(api1, api2, api3)

    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", List(CombinedApiCategory("CUSTOMS")), ApiType.REST_API, Some(PUBLIC))
    val combinedXmlApi3 = CombinedApi("displayName2", "serviceName2", List(CombinedApiCategory("VAT")), ApiType.XML_API, Some(PUBLIC))
    val combinedList = List(combinedRestApi1, combinedXmlApi3)

    "show correct title and options when no selectedAPis provided" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesSelectApiView.render(dropDownApis, Seq.empty, request, LoggedInUser(None), messagesProvider)
      
      validateSelectAPIPageWithNonePreviouslySelected(Jsoup.parse(result.body), dropDownApis)
    }
    
    "show correct title and options when selectedAPis are provided" in new Setup {
      val result: HtmlFormat.Appendable =
      emailPreferencesSelectApiView.render(dropDownApis, combinedList, request, LoggedInUser(None), messagesProvider)

      validateSelectAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), dropDownApis, combinedList)
    }
  }
}
