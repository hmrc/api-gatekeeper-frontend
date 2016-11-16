/*
 * Copyright 2016 HM Revenue & Customs
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

package unit.services

import connectors.ApplicationConnector
import model._
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import services.ApplicationService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val testApplicationService = new ApplicationService {
      val applicationConnector = mock[ApplicationConnector]
    }
    implicit val hc = HeaderCarrier()
  }

  "applicationService" should {
    "call applicationConnector with appropriate parameters" in new Setup {
      val applicationId = "applicationId"
      val userName = "userName"
      val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
      val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testApplicationService.applicationConnector.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ResendVerificationSuccessful))

      val result = await(testApplicationService.resendVerification(applicationId, userName))

      appIdCaptor.getValue shouldBe applicationId
      gatekeeperIdCaptor.getValue shouldBe userName
    }
  }
}
