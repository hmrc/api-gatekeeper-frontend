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

sealed trait AccessType {
  lazy val displayText: String = this.toString().toLowerCase().capitalize
}

object AccessType {
  case object STANDARD   extends AccessType
  case object PRIVILEGED extends AccessType
  case object ROPC       extends AccessType

  val values = List(STANDARD, PRIVILEGED, ROPC)

  def apply(text: String): Option[AccessType] = AccessType.values.find(_.toString() == text.toUpperCase)

  def unsafeApply(text: String): AccessType = apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid Access Type"))

  import play.api.libs.json.Format
  import uk.gov.hmrc.apiplatform.modules.common.domain.services.SealedTraitJsonFormatting
  implicit val format: Format[AccessType] = SealedTraitJsonFormatting.createFormatFor[AccessType]("Access Type", apply)

}
