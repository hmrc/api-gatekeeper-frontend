/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.services

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, LoginRedirectUri, PostLogoutRedirectUri}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.ApplicationCommands
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.services.{ApplicationLogger, ClockNow}
import uk.gov.hmrc.gatekeeper.connectors.ApplicationCommandConnector
import uk.gov.hmrc.gatekeeper.models.{ApplicationUpdateFailureResult, ApplicationUpdateResult, ApplicationUpdateSuccessResult}

@Singleton
class RedirectUrisService @Inject() (
    commandConnector: ApplicationCommandConnector,
    val clock: Clock
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger with ClockNow {

  def manageLoginRedirectUris(
      application: ApplicationWithCollaborators,
      redirectUris: List[LoginRedirectUri],
      gatekeeperUser: String
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {

    commandConnector.dispatch(
      application.id,
      ApplicationCommands.UpdateLoginRedirectUris(
        Actors.GatekeeperUser(gatekeeperUser),
        redirectUris,
        instant()
      ),
      Set.empty[LaxEmailAddress]
    )
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }

  def managePostLogoutRedirectUris(
      application: ApplicationWithCollaborators,
      redirectUris: List[PostLogoutRedirectUri],
      gatekeeperUser: String
    )(implicit hc: HeaderCarrier
    ): Future[ApplicationUpdateResult] = {

    commandConnector.dispatch(
      application.id,
      ApplicationCommands.UpdatePostLogoutRedirectUris(
        Actors.GatekeeperUser(gatekeeperUser),
        redirectUris,
        instant()
      ),
      Set.empty[LaxEmailAddress]
    )
      .map(_.fold(_ => ApplicationUpdateFailureResult, _ => ApplicationUpdateSuccessResult))
  }
}
