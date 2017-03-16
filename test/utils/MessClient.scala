/*
 * Copyright 2017 HM Revenue & Customs
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

package utils

import scalaj.http.{Http, HttpResponse}

trait MessClient {

  def getRequest(url: String): HttpResponse[String] = {
    val request = Http(url).asString
    request
  }

  def postRequest(url: String, payload: String): HttpResponse[String] = {
    val request = Http(url).postData(payload).asString
    request
  }

  def deleteRequest(url: String): HttpResponse[String] = {
    val request = Http(url).method("DELETE").asString
    request
  }
}
