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

package uk.gov.hmrc.apiplatform.modules.applications.access.domain.models

import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.RedirectUri
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models.ImportantSubmissionData

sealed trait Access {
  lazy val accessType: AccessType = Access.accessType(this)
}

object Access {

  def accessType(access: Access): AccessType = access match {
    case _: Standard   => AccessType.STANDARD
    case _: Privileged => AccessType.PRIVILEGED
    case _: Ropc       => AccessType.ROPC
  }

  case class Standard(
      redirectUris: List[RedirectUri] = List.empty,
      termsAndConditionsUrl: Option[String] = None,
      privacyPolicyUrl: Option[String] = None,
      overrides: Set[OverrideFlag] = Set.empty,
      sellResellOrDistribute: Option[SellResellOrDistribute] = None,
      importantSubmissionData: Option[ImportantSubmissionData] = None
    ) extends Access

  case class Privileged(
      totpIds: Option[TotpId] = None,
      scopes: Set[String] = Set.empty
    ) extends Access

  case class Ropc(scopes: Set[String] = Set.empty) extends Access

  import uk.gov.hmrc.play.json.Union

  private implicit val formatStandard: OFormat[Standard]     = Json.format[Standard]
  private implicit val formatPrivileged: OFormat[Privileged] = Json.format[Privileged]
  private implicit val formatRopc: OFormat[Ropc]             = Json.format[Ropc]

  implicit val format: OFormat[Access] = Union.from[Access]("accessType")
    .and[Standard](AccessType.STANDARD.toString)
    .and[Privileged](AccessType.PRIVILEGED.toString)
    .and[Ropc](AccessType.ROPC.toString)
    .format
}
