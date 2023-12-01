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

import java.net.URI
import scala.util.Try

case class RedirectUri(uri: String) extends AnyVal {
  override def toString(): String = uri
}

object RedirectUri {

  private def isValidRedirectUri(s: String): Boolean = {
    def isNotBlankString: String => Boolean = s => s.trim.nonEmpty
    def hasNoFragments: String => Boolean   = s => Try(new URI(s.trim).getFragment == null).getOrElse(false)
    def isLocalhostUrl: String => Boolean   = s => Try(new URI(s.trim).getHost == "localhost").getOrElse(false)
    def isNotHttpUrl: String => Boolean     = s => Try(new URI(s.trim).getScheme != "http").getOrElse(false)
    def isAbsoluteUrl: String => Boolean    = s => Try(new URI(s.trim).isAbsolute).getOrElse(false)

    isNotBlankString(s) && hasNoFragments(s) && isAbsoluteUrl(s) && (isLocalhostUrl(s) || isNotHttpUrl(s))
  }

  def apply(uri: String): Option[RedirectUri] = Some(new RedirectUri(uri)).filter(_ => isValidRedirectUri(uri))

  def unsafeApply(uri: String): RedirectUri =
    apply(uri).fold(throw new IllegalArgumentException(s"Bad format for URI `$uri`"))(identity)

  import play.api.libs.json._

  implicit val format: Format[RedirectUri] = Json.valueFormat[RedirectUri]

}
