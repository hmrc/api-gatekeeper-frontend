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

package uk.gov.hmrc.gatekeeper.support

import org.apache.pekko.stream.Materializer

import play.api.Application
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec

abstract class BaseISpec
    extends AsyncHmrcSpec with WireMockSupport with MetricsTestSupport {

  def app: Application
  protected def appBuilder: GuiceApplicationBuilder

  override def commonStubs(): Unit = {
    givenCleanMetricRegistry()
  }

  given Materializer = app.materializer

  private lazy val messagesApi = app.injector.instanceOf[MessagesApi]
  given Messages               = messagesApi.preferred(Seq.empty[Lang])

  protected def htmlEscapedMessage(key: String): String = HtmlFormat.escape(Messages(key)).toString

  given Conversion[FakeRequest[?], HeaderCarrier] with {
    def apply(request: FakeRequest[?]) = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
  }
}
