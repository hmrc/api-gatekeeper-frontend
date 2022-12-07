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

package uk.gov.hmrc.gatekeeper.controllers.ldap

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import scala.concurrent.Future.successful
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.internalauth.client._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationPredicate

@Singleton
class LdapController @Inject() (
    auth: FrontendAuthComponents,
    mcc: MessagesControllerComponents
  ) extends FrontendController(mcc) {

  def signIn = Action.async { implicit initialRequest =>
    auth.authorizedAction(
      continueUrl = uk.gov.hmrc.gatekeeper.controllers.routes.ApplicationController.applicationsPage(),
      predicate = LdapAuthorisationPredicate.gatekeeperReadPermission
    ).async { _ =>
      successful(Redirect(uk.gov.hmrc.gatekeeper.controllers.routes.ApplicationController.applicationsPage()))
    }(initialRequest)
  }

}
