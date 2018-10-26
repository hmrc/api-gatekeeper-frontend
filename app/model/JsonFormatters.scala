/*
 * Copyright 2018 HM Revenue & Customs
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

object JsonFormatters {
}

object EnumJson {

  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(s) => {
        try {
          JsSuccess(enum.withName(s))
        } catch {
          case _: NoSuchElementException =>
            throw new InvalidEnumException(enum.getClass.getSimpleName, s)
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(v: E#Value): JsValue = JsString(v.toString)
  }

  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
    Format(enumReads(enum), enumWrites)
  }

}

class InvalidEnumException(className: String, input:String) extends RuntimeException(s"Enumeration expected of type: '$className', but it does not contain '$input'")

object APIStatusJson {

  def apiStatusReads[APIStatus](apiStatus: APIStatus): Reads[APIStatus.Value] = new Reads[APIStatus.Value] {
    def reads(json: JsValue): JsResult[APIStatus.Value] = json match {
      case JsString("PROTOTYPED") => JsSuccess(APIStatus.BETA)
      case JsString("PUBLISHED") => JsSuccess(APIStatus.STABLE)
      case JsString(s) => {
        try {
          JsSuccess(APIStatus.withName(s))
        } catch {
          case _: NoSuchElementException =>
            JsError(s"Enumeration expected of type: APIStatus, but it does not contain '$s'")
        }
      }
      case _ => JsError("String value expected")
    }
  }

  implicit def apiStatusWrites: Writes[APIStatus.Value] = new Writes[APIStatus.Value] {
    def writes(v: APIStatus.Value): JsValue = JsString(v.toString)
  }

  implicit def apiStatusFormat[APIStatus](apiStatus: APIStatus): Format[APIStatus.Value] = {
    Format(apiStatusReads(apiStatus), apiStatusWrites)
  }

}