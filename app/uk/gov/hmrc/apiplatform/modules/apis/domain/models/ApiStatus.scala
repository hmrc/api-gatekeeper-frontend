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

import uk.gov.hmrc.apiplatform.modules.common.utils.SealedTraitJsonFormatting

sealed trait ApiStatus {
  lazy val displayText: String = {
    val txt = this.toString
    txt.take(1).toUpperCase +txt.drop(1).toLowerCase()
  }
}

object ApiStatus {
  case object ALPHA      extends ApiStatus
  case object BETA       extends ApiStatus
  case object STABLE     extends ApiStatus
  case object DEPRECATED extends ApiStatus
  case object RETIRED    extends ApiStatus

  final val values = Set(ALPHA, BETA, STABLE, DEPRECATED, RETIRED)

  def apply(text: String): Option[ApiStatus] = {
    ApiStatus.values.find(_.toString == text.toUpperCase)
  }

  def unsafeApply(text: String): ApiStatus =
    apply(text).getOrElse(throw new RuntimeException(s"$text is not a valid API Status"))
  
  implicit val formatApiStatus = SealedTraitJsonFormatting.createFormatFor[ApiStatus]("API Status", apply)
}
