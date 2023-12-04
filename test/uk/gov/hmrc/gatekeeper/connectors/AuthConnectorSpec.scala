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

package uk.gov.hmrc.gatekeeper.connectors

import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.config.AppConfig

class AuthConnectorSpec
    extends AsyncHmrcSpec
    with BeforeAndAfterEach
    with GuiceOneAppPerSuite {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val mockAppConfig = mock[AppConfig]
    val httpClient    = app.injector.instanceOf[HttpClient]

    val connector = new AuthConnector(httpClient, mockAppConfig)

    val url = "AUrl"

    when(mockAppConfig.authBaseUrl).thenReturn(url)
  }

  "auth connector" should {

    "get the base url from the app config" in new Setup {
      val result = connector.serviceUrl
      result shouldBe url
    }
  }
}
