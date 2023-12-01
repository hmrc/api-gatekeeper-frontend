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

package uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.play.json.Union

sealed trait ServerLocation

object ServerLocation {
  case object InUK                      extends ServerLocation
  case object InEEA                     extends ServerLocation
  case object OutsideEEAWithAdequacy    extends ServerLocation
  case object OutsideEEAWithoutAdequacy extends ServerLocation

  val values: List[ServerLocation] = List(InUK, InEEA, OutsideEEAWithAdequacy, OutsideEEAWithoutAdequacy)

  def apply(text: String): Option[ServerLocation] = ServerLocation.values.find(_.toString.toUpperCase == text.toUpperCase())

  def unsafeApply(text: String): ServerLocation = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Server Location"))

  // We cannot use SealedTraitJsonFormatting because this was never stored as an Enumeration or Enum originally but rather as an object
  //
  private implicit val inUkFormat: OFormat[InUK.type]                                           = Json.format[InUK.type]
  private implicit val inEEAFormat: OFormat[InEEA.type]                                         = Json.format[InEEA.type]
  private implicit val outsideEEAWithAdequacyFormat: OFormat[OutsideEEAWithAdequacy.type]       = Json.format[OutsideEEAWithAdequacy.type]
  private implicit val outsideEEAWithoutAdequacyFormat: OFormat[OutsideEEAWithoutAdequacy.type] = Json.format[OutsideEEAWithoutAdequacy.type]

  implicit val format: OFormat[ServerLocation] = Union.from[ServerLocation]("serverLocation")
    .and[InUK.type]("inUK")
    .and[InEEA.type]("inEEA")
    .and[OutsideEEAWithAdequacy.type]("outsideEEAWithAdequacy")
    .and[OutsideEEAWithoutAdequacy.type]("outsideEEAWithoutAdequacy")
    .format
}
