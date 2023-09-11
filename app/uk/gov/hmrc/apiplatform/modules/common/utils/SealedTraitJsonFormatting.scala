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

package uk.gov.hmrc.apiplatform.modules.common.utils

object SealedTraitJsonFormatting {
  import play.api.libs.json._

  def createFormatFor[T](name: String, read: String => Option[T], write: T => String = (t: T) => t.toString) = new Format[T] {

    def reads(json: JsValue): JsResult[T] = json match {
      case JsString(text) => read(text).fold[JsResult[T]] { JsError(s"$text is not a valid $name") }(JsSuccess(_))
      case e              => JsError(s"Cannot parse $name from '$e'")
    }

    def writes(foo: T): JsValue = {
      JsString(write(foo))
    }
  }
}
