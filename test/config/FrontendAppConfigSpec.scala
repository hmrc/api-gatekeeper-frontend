/*
 * Copyright 2021 HM Revenue & Customs
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

package config

import org.scalatestplus.play.guice.GuiceOneAppPerTest
import utils.AsyncHmrcSpec

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder

class AppConfigSpec extends AsyncHmrcSpec with GuiceOneAppPerTest {

  trait Setup {
    val appConfig = app.injector.instanceOf[AppConfig]
  }

  override def fakeApplication: Application = {
    new GuiceApplicationBuilder()
      .build()
  }

  "AppConfig" should {

    "be initialized with properties" in new Setup {
      appConfig.title shouldBe "HMRC API Gatekeeper"
    }
  }

}
