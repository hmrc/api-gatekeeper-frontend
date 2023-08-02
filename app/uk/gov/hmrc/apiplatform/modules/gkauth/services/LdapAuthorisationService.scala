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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.mvc._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.internalauth.client.{FrontendAuthComponents, _}
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.{GatekeeperRoles, LoggedInRequest}

@Singleton
class LdapAuthorisationService @Inject() (auth: FrontendAuthComponents)(implicit ec: ExecutionContext) extends ApplicationLogger {

  def refineLdap[A]: (MessagesRequest[A]) => Future[Either[MessagesRequest[A], LoggedInRequest[A]]] = (msgRequest) => {

    implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(msgRequest, msgRequest.session)

    lazy val notAuthenticatedOrAuthorized: Either[MessagesRequest[A], LoggedInRequest[A]] = Left(msgRequest)

    hc.authorization.fold({
      logger.debug("No Header Carrier Authoriation")
      successful(notAuthenticatedOrAuthorized)
    })(authorization => {
      auth.authConnector.authenticate(predicate = None, Retrieval.username ~ Retrieval.hasPredicate(LdapAuthorisationPredicate.gatekeeperReadPermission))
        .map {
          case (name ~ true)  => Right(new LoggedInRequest(name.value, GatekeeperRoles.READ_ONLY, msgRequest))
          case (name ~ false) =>
            logger.debug("No LDAP predicate matched")
            notAuthenticatedOrAuthorized
          case _              =>
            logger.debug("LDAP Authenticate failed to find user")
            notAuthenticatedOrAuthorized
        }
    })
  }
}
