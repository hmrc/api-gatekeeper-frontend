/*
 * Copyright 2020 HM Revenue & Customs
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

package model

import play.api.libs.json._

class InvalidEnumException(className: String, input:String) extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")

trait ApiStatusJson {
  def apiStatusReads[ApiStatus](apiStatus: ApiStatus): Reads[ApiStatus.Value] = new Reads[ApiStatus.Value] {
    def reads(json: JsValue): JsResult[ApiStatus.Value] = json match {
      case JsString("PROTOTYPED") => JsSuccess(ApiStatus.BETA)
      case JsString("PUBLISHED") => JsSuccess(ApiStatus.STABLE)
      case JsString(s) => {
        try {
          JsSuccess(ApiStatus.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: ApiStatus, but it does not contain '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def apiStatusWrites: Writes[ApiStatus.Value] = new Writes[ApiStatus.Value] {
    def writes(v: ApiStatus.Value): JsValue = JsString(v.toString)
  }

  implicit def apiStatusFormat[ApiStatus](apiStatus: ApiStatus): Format[ApiStatus.Value] = {
    Format(apiStatusReads(apiStatus), apiStatusWrites)
  }
}

object ApiStatusJson extends ApiStatusJson
