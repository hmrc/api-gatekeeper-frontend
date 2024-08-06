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

package uk.gov.hmrc.apiplatform.modules.applications.core.domain.models

import java.time.LocalDateTime

import play.api.libs.functional.syntax._
import play.api.libs.json._

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

case class GKApplicationResponse(
    id: ApplicationId,
    clientId: ClientId,
    gatewayId: String,
    name: String,
    deployedTo: Environment,
    description: Option[String],
    collaborators: Set[Collaborator],
    createdOn: LocalDateTime,
    lastAccess: Option[LocalDateTime],
    grantLength: GrantLength,
    termsAndConditionsUrl: Option[String],
    privacyPolicyUrl: Option[String],
    access: Access,
    state: ApplicationState,
    rateLimitTier: RateLimitTier,
    checkInformation: Option[CheckInformation],
    blocked: Boolean,
    ipAllowlist: IpAllowlist,
    redirectUris: List[RedirectUri] = List.empty,
    moreApplication: MoreApplication
  ) {

  lazy val admins: Set[Collaborator] = collaborators.filter(_.isAdministrator)

  lazy val developers: Set[Collaborator] = collaborators.filter(_.isDeveloper)
}

object GKApplicationResponse {

  val reads: Reads[GKApplicationResponse] = (
    (JsPath \ "id").read[ApplicationId] and
      (JsPath \ "clientId").read[ClientId] and
      (JsPath \ "gatewayId").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "deployedTo").read[Environment] and
      (JsPath \ "description").readNullable[String] and
      (JsPath \ "collaborators").read[Set[Collaborator]] and
      (JsPath \ "createdOn").read[LocalDateTime] and
      (JsPath \ "lastAccess").readNullable[LocalDateTime] and
      (JsPath \ "grantLength").read[GrantLength] and
      (JsPath \ "termsAndConditionsUrl").readNullable[String] and
      (JsPath \ "privacyAndPolicyUrl").readNullable[String] and
      (JsPath \ "access").read[Access] and
      (JsPath \ "state").read[ApplicationState] and
      (JsPath \ "rateLimitTier").read[RateLimitTier] and
      (JsPath \ "checkInformation").readNullable[CheckInformation] and
      (JsPath \ "blocked").readWithDefault[Boolean](false) and
      (JsPath \ "ipAllowlist").read[IpAllowlist] and
      (JsPath \ "redirectUris").readWithDefault[List[RedirectUri]](List.empty) and
      (JsPath \ "moreApplication").readWithDefault[MoreApplication](MoreApplication(true))
  )(GKApplicationResponse.apply _)

  val writes: OWrites[GKApplicationResponse] = Json.writes[GKApplicationResponse]

  implicit val format: Format[GKApplicationResponse] = Format(reads, writes)
}
