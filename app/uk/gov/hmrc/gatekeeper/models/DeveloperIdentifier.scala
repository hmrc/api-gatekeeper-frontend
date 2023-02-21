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

import scala.util.matching.Regex
import java.util.UUID
import scala.util.Try
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId

trait DeveloperIdentifier {
  def asText: String = DeveloperIdentifier.asText(this)
}
case class EmailIdentifier(val email: String) extends DeveloperIdentifier
case class UuidIdentifier(val userId: UserId) extends DeveloperIdentifier

object EmailIdentifier {
  private[this] val simplestEmailRegex: Regex = """^.+@.+\..+$""".r

  def parse(text: String): Option[EmailIdentifier] =
    simplestEmailRegex.findFirstIn(text).map(EmailIdentifier(_))

  implicit val format = Json.format[EmailIdentifier]
}

object UuidIdentifier {

  def parse(text: String): Option[UuidIdentifier] =
    Try(UUID.fromString(text)).toOption.map(u => UuidIdentifier(UserId(u)))
}

object DeveloperIdentifier {
  def apply(text: String): Option[DeveloperIdentifier] = EmailIdentifier.parse(text) orElse UuidIdentifier.parse(text)

  def asText(id: DeveloperIdentifier) = id match {
    case EmailIdentifier(email) => email
    case UuidIdentifier(id)     => id.value.toString
  }
}
