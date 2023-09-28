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

import java.util.UUID
import scala.util.Try
import scala.util.matching.Regex

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}

trait DeveloperIdentifier {
  def asText: String = DeveloperIdentifier.asText(this)
}
case class EmailIdentifier(val email: LaxEmailAddress) extends DeveloperIdentifier
case class UuidIdentifier(val userId: UserId) extends DeveloperIdentifier

object EmailIdentifier {
  private[this] val simplestEmailRegex: Regex = """^.+@.+\..+$""".r

  def parse(text: String): Option[EmailIdentifier] =
    simplestEmailRegex.findFirstIn(text).map(t => EmailIdentifier(LaxEmailAddress(t)))

  implicit val format = Json.format[EmailIdentifier]
}

object UuidIdentifier {

  def parse(text: String): Option[UuidIdentifier] =
    Try(UUID.fromString(text)).toOption.map(u => UuidIdentifier(UserId(u)))
}

object DeveloperIdentifier {
  def apply(text: String): Option[DeveloperIdentifier] = EmailIdentifier.parse(text) orElse UuidIdentifier.parse(text)

  def asText(id: DeveloperIdentifier) = id match {
    case EmailIdentifier(email) => email.text
    case UuidIdentifier(id)     => id.value.toString
  }
}
