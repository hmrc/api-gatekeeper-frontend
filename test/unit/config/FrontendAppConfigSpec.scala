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

package unit.config

import config.AppConfig
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.play.test.UnitSpec

class AppConfigSpec extends UnitSpec with GuiceOneAppPerTest with MockitoSugar {

  var isExternalTest = false

  trait Setup {
    val appConfig = app.injector.instanceOf[AppConfig]
  }

  override def fakeApplication: Application = {
    new GuiceApplicationBuilder()
      .configure("isExternalTestEnvironment" -> isExternalTest)
      .build()
  }

  "AppConfig" should {

    "be initialized with properties" in new Setup {
      appConfig.isExternalTestEnvironment shouldBe false
      appConfig.title shouldBe "HMRC API Gatekeeper"

      isExternalTest = true
    }

    "be initialized with properties for external test environment" in new Setup {
      appConfig.isExternalTestEnvironment shouldBe true
      appConfig.title shouldBe "HMRC API Gatekeeper - Developer Sandbox"
    }

  }

}
