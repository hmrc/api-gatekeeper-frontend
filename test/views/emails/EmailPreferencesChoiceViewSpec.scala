/*
 * Copyright 2021 HM Revenue & Customs
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
import uk.gov.hmrc.modules.stride.domain.models.LoggedInUser
import org.jsoup.Jsoup
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.twirl.api.Html
import utils.FakeRequestCSRFSupport._
import views.CommonViewSpec
import views.html.emails.EmailPreferencesChoiceView

class EmailPreferencesChoiceViewSpec extends CommonViewSpec with EmailPreferencesChoiceViewHelper {

  trait Setup extends AppConfigMock {
    implicit val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken

    val preferencesChoiceView: EmailPreferencesChoiceView = app.injector.instanceOf[EmailPreferencesChoiceView]
  }

  "email preferences choice view" must {
    "show correct title and options" in new Setup {
      val result: Html = preferencesChoiceView.render(request, LoggedInUser(None), messagesProvider)

      validateEmailPreferencesChoicePage(Jsoup.parse(result.body))
    }
  }

}
