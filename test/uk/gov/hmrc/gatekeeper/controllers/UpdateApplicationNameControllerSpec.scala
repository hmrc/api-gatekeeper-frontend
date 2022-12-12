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

package uk.gov.hmrc.gatekeeper.controllers

import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import mocks.services.ApplicationServiceMockProvider
import uk.gov.hmrc.gatekeeper.models.Forms.UpdateApplicationNameForm
import uk.gov.hmrc.gatekeeper.models._
import org.mockito.captor.ArgCaptor
import play.api.data.Form
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.utils.WithCSRFAddToken
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.html._
import uk.gov.hmrc.gatekeeper.views.html.applications.{
  ManageApplicationNameAdminListView,
  ManageApplicationNameSingleAdminView,
  ManageApplicationNameSuccessView,
  ManageApplicationNameView
}

import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

class UpdateApplicationNameControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = app.materializer

  val forbiddenView        = mock[ForbiddenView]
  val errorTemplate        = mock[ErrorTemplate]
  val apmService           = mock[ApmService]
  val errorHandler         = mock[ErrorHandler]
  val newAppNameSessionKey = "newApplicationName"

  trait Setup extends ControllerSetupBase with ApplicationServiceMockProvider {
    val csrfToken                            = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
    val manageApplicationNameView            = app.injector.instanceOf[ManageApplicationNameView]
    val manageApplicationNameAdminListView   = app.injector.instanceOf[ManageApplicationNameAdminListView]
    val manageApplicationNameSingleAdminView = app.injector.instanceOf[ManageApplicationNameSingleAdminView]
    val manageApplicationNameSuccessView     = app.injector.instanceOf[ManageApplicationNameSuccessView]

    override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken

    val updateApplicationNameFormCaptor = ArgCaptor[Form[UpdateApplicationNameForm]]
    val validName                       = "valid app name"
    val appId                           = ApplicationId.random

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
      StrideAuthorisationServiceMock.aMock
    )
  }

  "updateApplicationNamePage" should {
    "display page correctly" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNamePage(appId)(aLoggedInRequest)

      status(result) shouldBe OK
    }
  }

  "updateApplicationNameAction" should {
    "redirect to the admin email page if the app name is valid" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.ValidateApplicationName.succeeds()
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> "my app name"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${appId.value.toString()}/name/admin-email")
    }

    "redisplay the name entry page if the name has not changed" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> basicApplication.name))

      status(result) shouldBe OK
      contentAsString(result) should include("The application already has the specified name")
    }

    "redisplay the name entry page if the name is invalid" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.ValidateApplicationName.invalid()
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> "some invalid name"))

      status(result) shouldBe OK
      contentAsString(result) should include("The application name is invalid")
    }

    "redisplay the name entry page if the name is a duplicate" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.ValidateApplicationName.duplicate()
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAction(appId)(aLoggedInRequest.withFormUrlEncodedBody("applicationName" -> "some duplicate name"))

      status(result) shouldBe OK
      contentAsString(result) should include("An application with this name already exists")
    }
  }

  "updateApplicationNameAdminEmailPage" should {
    "display single admin page if there is only 1 admin for the app" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAdminEmailPage(appId)(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("Confirm who changed the application name")
    }

    "display multiple admin page if there is > 1 admin for the app" in new Setup {
      val appWithMultipleAdmins = basicApplication.copy(collaborators =
        Set("sample@example.com".asAdministratorCollaborator, "someone@example.com".asDeveloperCollaborator, "another@example.com".asAdministratorCollaborator)
      )

      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(appWithMultipleAdmins, List.empty))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAdminEmailPage(appId)(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("Which admin requested the application name change?")
    }
  }

  "updateApplicationNameAdminEmailAction" should {
    "redirect to the success page if app name update succeeds" in new Setup {
      val appNameRequest = aLoggedInRequest.withSession(newAppNameSessionKey -> "New app name")
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.UpdateApplicationName.succeeds
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAdminEmailAction(appId)(appNameRequest.withFormUrlEncodedBody("adminEmail" -> "admin@example.com"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${appId.value.toString()}/name/updated")
    }

    "display app name entry page with an error if app name update fails" in new Setup {
      val appNameRequest = aLoggedInRequest.withSession(newAppNameSessionKey -> "New app name")
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      ApplicationServiceMock.UpdateApplicationName.fails
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameAdminEmailAction(appId)(appNameRequest.withFormUrlEncodedBody("adminEmail" -> "admin@example.com"))

      status(result) shouldBe OK
      contentAsString(result) should include("Failed to update the application name")
    }
  }

  "updateApplicationNameSuccessPage" should {
    "display the success page with new app name" in new Setup {
      ApplicationServiceMock.FetchApplication.returns(ApplicationWithHistory(basicApplication, List.empty))
      StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)

      val result = underTest.updateApplicationNameSuccessPage(appId)(aLoggedInRequest)

      status(result) shouldBe OK
      contentAsString(result) should include("Application name is now")
    }
  }

}
