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

package uk.gov.hmrc.gatekeeper.views

import java.util.Locale

import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.i18n.{Lang, MessagesImpl, MessagesProvider}
import play.api.mvc.request.RequestAttrKey
import play.api.mvc.{AnyContentAsEmpty, MessagesControllerComponents, MessagesRequest, Request}
import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._

trait CommonViewSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {
  val mcc                                         = app.injector.instanceOf[MessagesControllerComponents]
  val messagesApi                                 = mcc.messagesApi
  implicit val messagesProvider: MessagesProvider = MessagesImpl(Lang(Locale.ENGLISH), messagesApi)

  val developer = Developer(RegisteredUser(LaxEmailAddress("email@example.com"), UserId.random, "firstname", "lastName", true), List.empty)

  implicit val fakeRequest: Request[AnyContentAsEmpty.type] = FakeRequest().withCSRFToken.addAttr(RequestAttrKey.CSPNonce, "fake-nonce")

  val msgRequest        = new MessagesRequest(fakeRequest, messagesApi)
  val strideUserRequest = new LoggedInRequest(Some(developer.user.fullName), GatekeeperRoles.USER, msgRequest)
  val superUserRequest  = new LoggedInRequest(Some(developer.user.fullName), GatekeeperRoles.SUPERUSER, msgRequest)
  val adminRequest      = new LoggedInRequest(Some(developer.user.fullName), GatekeeperRoles.ADMIN, msgRequest)
}
