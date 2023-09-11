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

package uk.gov.hmrc.apiplatform.modules.apis.domain.models

import play.api.libs.json.Format

import uk.gov.hmrc.apiplatform.modules.common.utils.SealedTraitJsonFormatting

sealed trait ApiVersionSource

object ApiVersionSource {

  case object RAML    extends ApiVersionSource
  case object OAS     extends ApiVersionSource
  case object UNKNOWN extends ApiVersionSource

  val values = Set(RAML, OAS, UNKNOWN)

  import cats.implicits._

  def apply(text: String): Option[ApiVersionSource] = text.toUpperCase match {
    case "RAML"    => ApiVersionSource.RAML.some
    case "OAS"     => ApiVersionSource.OAS.some
    case "UNKNOWN" => ApiVersionSource.UNKNOWN.some
    case _         => None
  }

  def unsafeApply(text: String): ApiVersionSource = {
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid API Version Source"))
  }

  implicit val formatApiVersionSource: Format[ApiVersionSource] = SealedTraitJsonFormatting.createFormatFor[ApiVersionSource]("API Version Source", apply)
}
