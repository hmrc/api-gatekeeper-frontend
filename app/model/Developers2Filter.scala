/*
 * Copyright 2019 HM Revenue & Customs
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

case class Developers2Filter(maybeEmailFilter: Option[String] = None, maybeApiFilter: Option[ApiContextVersion] = None)

case class ApiContextVersion(context: String, version: String){
  def toStringValue : String = s"${context}__$version"
}

object ApiContextVersion {
  private val ApiIdPattern = """^(.+)__(.+?)$""".r

  def apply(value: Option[String]): Option[ApiContextVersion] = {
    value match {
      case None => None
      case Some(ApiIdPattern(context, version)) => Some(ApiContextVersion(context, version))
      case _ => throw new Exception("Invalid API context or version")
    }
  }
}