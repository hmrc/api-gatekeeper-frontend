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

import play.api.libs.json.JsArray
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiAccessType
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.EmailPreferencesSubscribedApiView
import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory

class EmailPreferencesSubscribedApiViewSpec extends CommonViewSpec with EmailAPISubscriptionsViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type]                = FakeRequest().withCSRFToken
    val emailPreferencesSubscribedApiView: EmailPreferencesSubscribedApiView = app.injector.instanceOf[EmailPreferencesSubscribedApiView]
  }

  "email preferences subscribed api view" must {

    val combinedRestApi1 = CombinedApi("displayName1", "serviceName1", Set(ApiCategory.CUSTOMS), ApiType.REST_API, Some(ApiAccessType.PUBLIC))
    val combinedXmlApi2  = CombinedApi("displayName2", "serviceName2", Set(ApiCategory.VAT), ApiType.XML_API, Some(ApiAccessType.PUBLIC))
    val combinedList     = List(combinedRestApi1, combinedXmlApi2)

    val user1 = RegisteredUser("user1@hmrc.com".toLaxEmail, UserId.random, "userA", "1", verified = true)
    val user2 = RegisteredUser("user2@hmrc.com".toLaxEmail, UserId.random, "userB", "2", verified = true)
    val users = List(user1, user2)

    "render as expected and show correct title and elements" in new Setup {
      val result: HtmlFormat.Appendable = {
        emailPreferencesSubscribedApiView.render(combinedList, request, LoggedInUser(None), messagesProvider)
      }
      validateEmailPreferencesSubscribedApiPage(Jsoup.parse(result.body), combinedList)
    }
  }
}
