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

package uk.gov.hmrc.gatekeeper.utils

import scala.concurrent.Future

import akka.stream.Materializer

import play.api.mvc.Result
import play.api.test.Helpers._

import uk.gov.hmrc.apiplatform.modules.common.utils._

trait TitleChecker extends AsyncHmrcSpec {

  implicit val materializer: Materializer

  def titleOf(result: Future[Result]): String = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title      = titleRegEx.findFirstMatchIn(contentAsString(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }

  def titleOf(result: Result): String = titleOf(Future.successful(result))
}
