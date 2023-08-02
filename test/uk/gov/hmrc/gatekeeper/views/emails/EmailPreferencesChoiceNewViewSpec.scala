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
import play.twirl.api.Html

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInUser
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.emails.{EmailPreferencesChoiceNewView, EmailPreferencesChoiceView}

class EmailPreferencesChoiceNewViewSpec extends CommonViewSpec with EmailPreferencesChoiceViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val preferencesChoiceNewView: EmailPreferencesChoiceNewView = app.injector.instanceOf[EmailPreferencesChoiceNewView]
  }

  "email preferences choice view" must {
    "show correct title and options" in new Setup {
      val result: Html = preferencesChoiceNewView.render(request, LoggedInUser("Bobby Example"), messagesProvider)

      validateEmailPreferencesChoicePage(Jsoup.parse(result.body))
    }
  }

}
