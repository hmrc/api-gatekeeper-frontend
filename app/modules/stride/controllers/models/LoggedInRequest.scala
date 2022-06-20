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

package uk.gov.hmrc.modules.stride.controllers.models

import play.api.mvc.MessagesRequest
import uk.gov.hmrc.modules.stride.domain.models.GatekeeperRole
import uk.gov.hmrc.modules.stride

class LoggedInRequest[A](val name: Option[String], val role: GatekeeperRole.GatekeeperRole, request: MessagesRequest[A]) extends MessagesRequest[A](request, request.messagesApi) {
  lazy val isAtLeastSuperUser: Boolean = role == stride.domain.models.GatekeeperRole.SUPERUSER || isAdmin

  lazy val isAdmin: Boolean = role == GatekeeperRole.ADMIN
  private lazy val isSuperUse: Boolean = role == GatekeeperRole.SUPERUSER
}
