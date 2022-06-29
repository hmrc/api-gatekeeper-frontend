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

package uk.gov.hmrc.modules.stride.domain.models

sealed trait GatekeeperRole {
  def isAdmin: Boolean
  def isSuperUser: Boolean
  def isUser: Boolean
}

sealed trait GatekeeperStrideRole extends GatekeeperRole

object GatekeeperRoles {
  case object READ_ONLY extends GatekeeperRole {
    val isAdmin: Boolean = false
    val isSuperUser: Boolean = false
    val isUser: Boolean = false
  }

  case object USER extends GatekeeperStrideRole {
    val isAdmin: Boolean = false
    val isSuperUser: Boolean = false
    val isUser: Boolean = true
  }

  case object SUPERUSER extends GatekeeperStrideRole {
    val isAdmin: Boolean = false
    val isSuperUser: Boolean = true
    val isUser: Boolean = true
  }

  case object ADMIN extends GatekeeperStrideRole {
    val isAdmin: Boolean = true
    val isSuperUser: Boolean = true
    val isUser: Boolean = true
  }
}