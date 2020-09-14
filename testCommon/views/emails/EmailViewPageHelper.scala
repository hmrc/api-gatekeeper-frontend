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

import model.EmailOptionChoice.{API_SUBSCRIPTION, EMAIL_ALL_USERS, EMAIL_PREFERENCES}
import org.jsoup.nodes.Document
import utils.ViewHelpers.{elementExistsByIdWithAttr, elementExistsByText}


trait EmailLandingViewHelper extends EmailUsersHelper{

  def validateLandingPage(document: Document): Unit = {
    validatePageHeader(document, "Send emails to users based on")

    verifyEmailOptions(EMAIL_PREFERENCES, document, isDisabled = false)
    verifyEmailOptions(API_SUBSCRIPTION, document, isDisabled = false)
    verifyEmailOptions(EMAIL_ALL_USERS, document, isDisabled = false)
    elementExistsByIdWithAttr(document, EMAIL_PREFERENCES.toString, "checked") mustBe true

    validateButtonText(document, "submit", "Continue")
  }

}
