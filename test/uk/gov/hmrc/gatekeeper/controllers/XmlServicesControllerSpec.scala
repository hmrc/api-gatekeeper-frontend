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

package uk.gov.hmrc.gatekeeper.controllers

import scala.concurrent.ExecutionContext.Implicits.global

import play.api.test.Helpers._

import uk.gov.hmrc.gatekeeper.controllers.XmlServicesController
class XmlServicesControllerSpec extends ControllerBaseSpec {
  implicit val materializer = app.materializer

  running(app) {
    trait Setup extends ControllerSetupBase {
      val underTest = new XmlServicesController(mcc)
    }

    "xmlServicesController" should {

      "render XML nav link for standard site" in new Setup {
        val result = underTest.organisationsSearchPage()(aLoggedInRequest)
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).map(x => x.contains("api-gatekeeper-xml-services/organisations") shouldBe true)
      }
    }
  }
}
