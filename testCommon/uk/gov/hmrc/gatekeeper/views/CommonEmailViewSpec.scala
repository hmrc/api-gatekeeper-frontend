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

package uk.gov.hmrc.gatekeeper.views

import java.util.Locale

import play.api.i18n.{Lang, MessagesImpl, MessagesProvider}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.views.emails.EmailLandingViewHelper
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

trait CommonEmailViewSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite with EmailLandingViewHelper {
  val mcc = app.injector.instanceOf[MessagesControllerComponents]
  val messagesApi = mcc.messagesApi
  implicit val messagesProvider: MessagesProvider = MessagesImpl(Lang(Locale.ENGLISH), messagesApi)
}
