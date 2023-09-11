/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.views.emails

import mocks.config.AppConfigMock
import org.jsoup.Jsoup

import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.models.{ApiType, CombinedApi, CombinedApiCategory}
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesSelectSubscribedApiView
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._

class EmailPreferencesSelectSubscribedApiViewSpec extends CommonViewSpec with EmailPreferencesSelectAPIViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val emailPreferencesSelectSubscribedApiView: EmailPreferencesSelectSubscribedApiView = app.injector.instanceOf[EmailPreferencesSelectSubscribedApiView]
  }

  "email preferences subscribed api view" must {

    val api1: CombinedApi = simpleAPI(serviceName = "serviceName0", displayName = "displayName0", List.empty, ApiType.REST_API, Some(ApiAccessType.PUBLIC))
    val api2              = simpleAPI(serviceName = "serviceName1", displayName = "displayName1", List.empty, ApiType.REST_API, Some(ApiAccessType.PUBLIC))
    val api3              = simpleAPI(serviceName = "serviceName2", displayName = "displayName2", List.empty, ApiType.XML_API, Some(ApiAccessType.PUBLIC))
    val apis              = Seq(api1, api2, api3)

    val combinedRestApi1    = CombinedApi("displayName1", "serviceName1", List(CombinedApiCategory("CUSTOMS")), ApiType.REST_API, Some(ApiAccessType.PUBLIC))
    val combinedXmlApi3     = CombinedApi("displayName2", "serviceName2", List(CombinedApiCategory("VAT")), ApiType.XML_API, Some(ApiAccessType.PUBLIC))
    val combinedList        = List(combinedRestApi1, combinedXmlApi3)
    val expectedDestination = "/api-gatekeeper/emails/email-preferences/by-subscribed-api"

    "show correct title and options when no selectedAPIs provided" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectSubscribedApiView.render(apis, Seq.empty, request, LoggedInUser(None), messagesProvider)

      validateSelectAPIPageWithNonePreviouslySelectedNew(Jsoup.parse(result.body), apis, expectedDestination)
    }

    "show correct title and options when selectedAPIs are provided" in new Setup {
      val result: HtmlFormat.Appendable =
        emailPreferencesSelectSubscribedApiView.render(apis, combinedList, request, LoggedInUser(None), messagesProvider)

      validateSelectAPIPageWithPreviouslySelectedAPIs(Jsoup.parse(result.body), apis, combinedList, expectedDestination)
    }
  }
}
