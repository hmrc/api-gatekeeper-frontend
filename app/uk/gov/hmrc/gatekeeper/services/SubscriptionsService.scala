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

package uk.gov.hmrc.gatekeeper.services

import java.time.Clock
import javax.inject.{Inject, Singleton}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommands, CommandHandlerTypes, DispatchSuccessResult}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.gatekeeper.connectors.ApplicationCommandConnector
import uk.gov.hmrc.gatekeeper.models.Application
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

@Singleton
class SubscriptionsService @Inject() (
    applicationCommandConnector: ApplicationCommandConnector,
    val clock: Clock
  ) extends CommandHandlerTypes[DispatchSuccessResult]
    with ClockNow {

  def subscribeToApi(application: Application, apiIdentifier: ApiIdentifier, user: Actors.GatekeeperUser)(implicit hc: HeaderCarrier): AppCmdResult = {
    val cmd = ApplicationCommands.SubscribeToApi(user, apiIdentifier, now())
    applicationCommandConnector.dispatch(application.id, cmd, Set.empty)
  }

  def unsubscribeFromApi(application: Application, apiIdentifier: ApiIdentifier, user: Actors.GatekeeperUser)(implicit hc: HeaderCarrier): AppCmdResult = {
    val cmd = ApplicationCommands.UnsubscribeFromApi(user, apiIdentifier, now())
    applicationCommandConnector.dispatch(application.id, cmd, Set.empty)
  }
}
