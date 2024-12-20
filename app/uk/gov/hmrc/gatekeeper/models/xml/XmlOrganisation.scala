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

package uk.gov.hmrc.gatekeeper.models.xml

import play.api.libs.json.{Format, Json, OFormat}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}

case class OrganisationId(value: java.util.UUID) extends AnyVal

object OrganisationId {
  implicit val formatOrganisationId: Format[OrganisationId] = Json.valueFormat[OrganisationId]
}

case class VendorId(value: Long) extends AnyVal

object VendorId {
  implicit val formatVendorId: Format[VendorId] = Json.valueFormat[VendorId]
}

case class Collaborator(userId: UserId, email: LaxEmailAddress)

object Collaborator {
  implicit val formatCollaborator: OFormat[Collaborator] = Json.format[Collaborator]
}

case class XmlOrganisation(organisationId: OrganisationId, vendorId: VendorId, name: String, collaborators: List[Collaborator])

object XmlOrganisation {
  implicit val formatOrganisation: OFormat[XmlOrganisation] = Json.format[XmlOrganisation]
}
