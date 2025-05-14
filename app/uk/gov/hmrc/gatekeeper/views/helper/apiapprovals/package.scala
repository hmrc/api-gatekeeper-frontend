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

package uk.gov.hmrc.gatekeeper.views.helper

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actor
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors.{GatekeeperUser, Process}
import uk.gov.hmrc.gatekeeper.models.ApprovalStatus.{APPROVED, FAILED, NEW, RESUBMITTED}
import uk.gov.hmrc.gatekeeper.models.{ApiApprovalState, ApprovalStatus}

object DateFormatter {

  val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")

  def getFormattedDate(date: Option[Instant]): String = {
    date match {
      case None             => ""
      case Some(d: Instant) => dateFormatter.format(d.atOffset(ZoneOffset.UTC).toLocalDateTime)
    }
  }
}

object ExtractUser {

  def displayUser(actor: Actor): String = {
    actor match {
      case GatekeeperUser(user) => user
      case Process(_)           => "Producer team"
      case _                    => ""
    }
  }
}

object StatusWording {

  def displayStatus(status: Option[ApprovalStatus]): String = {
    status match {
      case Some(NEW)         => "API approval request submitted"
      case Some(RESUBMITTED) => "API approval request submitted"
      case Some(FAILED)      => "API approval request rejected"
      case Some(APPROVED)    => "API approval request approved"
      case None              => "Update"
    }
  }
}

object HistoryOrdering {

  def changedAtOrdering: (ApiApprovalState, ApiApprovalState) => Boolean = {
    (first, second) => first.changedAt.isAfter(second.changedAt)
  }
}
