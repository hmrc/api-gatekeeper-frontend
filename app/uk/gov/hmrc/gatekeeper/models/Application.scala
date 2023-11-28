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

package uk.gov.hmrc.gatekeeper.models

import java.time.LocalDateTime

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.utils.PaginationHelper

trait Application {
  val id: ApplicationId
  val name: String
  val state: ApplicationState
  val collaborators: Set[Collaborator]
  val clientId: ClientId
  val deployedTo: Environment
}

case class PaginatedApplicationResponse(applications: List[ApplicationResponse], page: Int, pageSize: Int, total: Int, matching: Int) {
  val maxPage = PaginationHelper.maxPage(matching, pageSize)
}

object PaginatedApplicationResponse {
  implicit val format: OFormat[PaginatedApplicationResponse] = Json.format[PaginatedApplicationResponse]
}

case class ApplicationWithSubscriptionsResponse(id: ApplicationId, name: String, lastAccess: Option[LocalDateTime], apiIdentifiers: Set[ApiIdentifier])

object ApplicationWithSubscriptionsResponse {
  implicit val format: Format[ApplicationWithSubscriptionsResponse] = Json.format[ApplicationWithSubscriptionsResponse]
}

case class TotpSecrets(production: String)

case class SubscriptionNameAndVersion(name: String, version: String)
