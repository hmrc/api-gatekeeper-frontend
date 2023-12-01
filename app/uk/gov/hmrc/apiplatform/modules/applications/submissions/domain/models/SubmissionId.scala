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

import java.util.UUID
import scala.util.control.Exception._

case class SubmissionId(value: UUID) extends AnyVal {
  override def toString(): String = value.toString()
}

object SubmissionId {
  import play.api.libs.json.{Format, Json}

  def apply(raw: String): Option[SubmissionId] = allCatch.opt(SubmissionId(UUID.fromString(raw)))

  def unsafeApply(raw: String): SubmissionId = SubmissionId(UUID.fromString(raw))

  implicit val format: Format[SubmissionId] = Json.valueFormat[SubmissionId]

// $COVERAGE-OFF$
  def random: SubmissionId = SubmissionId(UUID.randomUUID)
// $COVERAGE-ON$
}
