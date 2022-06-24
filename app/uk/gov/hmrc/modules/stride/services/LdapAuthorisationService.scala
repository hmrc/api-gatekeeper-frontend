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

package uk.gov.hmrc.modules.stride.services

import uk.gov.hmrc.internalauth.client.FrontendAuthComponents
import play.api.mvc.MessagesRequest
import scala.concurrent.Future
import scala.concurrent.Future.successful
import uk.gov.hmrc.play.http.HeaderCarrierConverter
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.modules.stride.domain.models.LoggedInRequest
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRoles
import uk.gov.hmrc.internalauth.client._
import scala.concurrent.ExecutionContext

class LdapAuthorisationService(implicit ec: ExecutionContext) {
  def refineLdap[A](auth: FrontendAuthComponents)(msgRequest: MessagesRequest[A]): Future[Option[LoggedInRequest[A]]] = {
    val gatekeeperPermission = Predicate.Permission(
      Resource(
        ResourceType("api-gatekeeper-frontend"),
        ResourceLocation("*")
      ),
      IAAction("READ")
    )

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(msgRequest, msgRequest.session)

    val notAuthenticatedOrAuthorized: Future[Option[LoggedInRequest[A]]] = successful(None)

    hc.authorization.fold(notAuthenticatedOrAuthorized)(authorization => {
      auth.authConnector.authenticate(predicate = None, Retrieval.username ~ Retrieval.hasPredicate(gatekeeperPermission))
        .map[Option[LoggedInRequest[A]]] {
          case (name ~ hasPredicate) => if(hasPredicate) Some(new LoggedInRequest(Some(name.value), GatekeeperRoles.READ_ONLY, msgRequest)) else None
          case _ => None
        }
    })
  }
}
