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
import play.api.test.{FakeApplication, TestServer}
import uk.gov.hmrc.play.test.UnitSpec

class AppConfigSpec extends UnitSpec {
  var port = sys.env.getOrElse("MICROSERVICE_PORT", "9001").toInt

  "appContext" should {
    "be initialized with properties" in {
      val app = FakeApplication(additionalConfiguration = Map())
      val server = TestServer(port, app)
      server.start()
      AppConfig.nameDisplayLimit shouldBe 20
      server.stop()
    }
  }

}
