/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.ErrorHandler
import mocks.connectors.AuthConnectorMock
import mocks.services.ApplicationServiceMockProvider
import model.Forms.UpdateApplicationNameForm
import model._
import org.mockito.captor.ArgCaptor
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import services.ApmService
import utils.WithCSRFAddToken
import views.html._
import views.html.applications.{ManageApplicationNameAdminListView, ManageApplicationNameSingleAdminView, ManageApplicationNameSuccessView, ManageApplicationNameView}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateApplicationNameControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {
      
  implicit val materializer = app.materializer

  val forbiddenView = mock[ForbiddenView]
  val errorTemplate = mock[ErrorTemplate]
  val manageApplicationNameView = mock[ManageApplicationNameView]
  val manageApplicationNameAdminListView = mock[ManageApplicationNameAdminListView]
  val manageApplicationNameSingleAdminView = mock[ManageApplicationNameSingleAdminView]
  val manageApplicationNameSuccessView = mock[ManageApplicationNameSuccessView]
  val apmService = mock[ApmService]
  val errorHandler = mock[ErrorHandler]

  when(manageApplicationNameView.apply(*,*)(*,*,*)).thenReturn(play.twirl.api.HtmlFormat.empty)

  trait Setup extends ControllerSetupBase with AuthConnectorMock with ApplicationServiceMockProvider {
    val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)

    val updateApplicationNameFormCaptor = ArgCaptor[Form[UpdateApplicationNameForm]]
    val validName = "valid app name"
    val appId = ApplicationId.random
    val underTest = new UpdateApplicationNameController(
      mockApplicationService,
      forbiddenView,
      mcc,
      errorTemplate,
      manageApplicationNameView,
      manageApplicationNameAdminListView,
      manageApplicationNameSingleAdminView,
      manageApplicationNameSuccessView,
      apmService,
      errorHandler,
      mockAuthConnector,
      forbiddenHandler
    )
  }

  "updateApplicationNamePage" should {
    "display page correctly" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = underTest.updateApplicationNamePage(appId)(aLoggedInRequest)

      status(result) shouldBe OK
    }
  }

  "updateApplicationNameAction" should {
    "redirect to the admin email page if the app name is valid" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.ValidateApplicationName.succeeds()
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> "my app name"))

      redirectLocation(result) shouldBe  Some(s"/api-gatekeeper/applications/${appId.value}/name/admin-email")
    }

    "redisplay the name entry page if the name has not changed" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      givenTheGKUserIsAuthorisedAndIsANormalUser()

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> basicApplication.name))

      status(result) shouldBe OK

      verify(manageApplicationNameView, times(2)).apply(*, updateApplicationNameFormCaptor)(*,*,*)
      val form = updateApplicationNameFormCaptor.value
      form.errors.size shouldBe 1
      form.error("applicationName").get.message shouldBe "The application already has the specified name"
    }

    "redisplay the name entry page if the name is invalid" in new Setup {

    }

    "redisplay the name entry page if the name is a duplicate" in new Setup {

    }

  }

  "updateApplicationNameAdminEmailPage" should {
    "display single admin page if there is only 1 admin for the app" in new Setup {

    }
    "display multiple admin page if there is > 1 admin for the app" in new Setup {

    }
  }

  "updateApplicationNameAdminEmailAction" should {
    "redirect to the success page if app name update succeeds" in new Setup {

    }
    "display app name entry page with an error if app name update fails" in new Setup {

    }
  }

  "updateApplicationNameSuccessPage" should {
    "display the success page with new app name" in new Setup {

    }
  }

}
