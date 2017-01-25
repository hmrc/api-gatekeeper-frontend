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

package unit.config

import config.AppConfig
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.OneServerPerTest
import play.api.{Configuration}
import uk.gov.hmrc.play.test.UnitSpec

class AppConfigSpec extends UnitSpec with OneServerPerTest with MockitoSugar {

  "appContext" should {
    "be initialized with properties" in {
      AppConfig.nameDisplayLimit shouldBe 20
      AppConfig.isExternalTestEnvironment should be(false)
      AppConfig.title should be("API Gatekeeper")
    }
  }

  "appContext" should {
    "be initialized with properties for external test environment" in {
      val mockConfiguration = mock[Configuration]
      when(mockConfiguration.getBoolean("isExternalTestEnvironment")).thenReturn(Some(true))

      val appConfig = new AppConfig {
        override protected val configuration: Configuration = mockConfiguration
      }

      appConfig.isExternalTestEnvironment should be(true)
      appConfig.title should be("API Gatekeeper - Developer Sandbox")
    }
  }
}
