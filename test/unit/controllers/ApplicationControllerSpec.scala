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

package unit.controllers

import connectors.AuthConnector.InvalidCredentials
import controllers.ApplicationController
import model._
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val materializer = fakeApplication.materializer

  running(fakeApplication) {

    trait Setup extends ControllerSetupBase {

      val underTest = new ApplicationController {
        override val appConfig = mockConfig
        val authConnector = mockAuthConnector
        val authProvider = mockAuthProvider
        val applicationService = mockApplicationService
        val apiDefinitionConnector = mockApiDefinitionConnector
      }
    }

    "applicationController" should {

      "on request all applications supplied" in new Setup {
        givenASuccessfulLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")
        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)
        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody should include("<a class=\"tabs-nav__tab\" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        responseBody should include("<span class=\"tabs-nav__tab tabs-nav__tab--active\">Applications</span>")
        responseBody should include("<a class=\"tabs-nav__tab\" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "not show Dashboard tab in external test mode" in new Setup {
        givenASuccessfulLogin
        val allSubscribedApplications: Seq[SubscribedApplicationResponse] = Seq.empty
        given(mockApplicationService.fetchAllSubscribedApplications(any[HeaderCarrier])).willReturn(Future(allSubscribedApplications))
        given(mockApiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")
        given(mockConfig.isExternalTestEnvironment).willReturn(true)
        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)
        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody shouldNot include("<a class=\"tabs-nav__tab\" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        responseBody should include("<span class=\"tabs-nav__tab tabs-nav__tab--active\">Applications</span>")
        responseBody should include("<a class=\"tabs-nav__tab\" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        givenAUnsuccessfulLogin
        val result = await(underTest.applicationsPage(aLoggedInRequest))
        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
      }

      "go to loginpage with error if user is not authenticated" in new Setup {
        given(underTest.authConnector.login(any[LoginDetails])(any[HeaderCarrier]))
          .willReturn(Future.failed(new InvalidCredentials))
        val result = await(underTest.applicationsPage(aLoggedOutRequest))
        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }

      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        val applicationId = "applicationId"
        givenASuccessfulLogin
        val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(underTest.applicationService.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ResendVerificationSuccessful))
        val result = await(underTest.resendVerification(applicationId)(aLoggedInRequest))
        appIdCaptor.getValue shouldBe applicationId
        gatekeeperIdCaptor.getValue shouldBe userName
      }
    }
  }

  def titleOf(result: Result) = {
    val titleRegEx = """<title[^>]*>(.*)</title>""".r
    val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
    title.isDefined shouldBe true
    title.get
  }
}
