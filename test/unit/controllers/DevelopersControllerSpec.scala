/*
 * Copyright 2018 HM Revenue & Customs
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

import java.util.UUID

import connectors.AuthConnector.InvalidCredentials
import controllers.DevelopersController
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.utils.WithCSRFAddToken

import scala.concurrent.Future

class DevelopersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  Helpers.running(fakeApplication) {

    def anApplication(collaborators: Set[Collaborator]) = {
      ApplicationResponse(UUID.randomUUID(), "application", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())
    }

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)
      given(mockConfig.superUsers).willReturn(Seq("superUserName"))

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new DevelopersController {
        val authConnector = mockAuthConnector
        val authProvider = mockAuthProvider
        val apiDefinitionService = mockApiDefinitionService
        val developerService = mockDeveloperService
        val applicationService = mockApplicationService
        override val appConfig = mockConfig
      }

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noUsers, noUsers)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], users: Seq[ApplicationDeveloper], developers: Seq[ApplicationDeveloper]): Unit = {
        val apiFiler = ApiFilter(None)
        val statusFilter = StatusFilter(None)
        given(mockApplicationService.fetchApplications(eqTo(apiFiler))(any[HeaderCarrier])).willReturn(Future.successful(apps))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockDeveloperService.filterUsersBy(apiFiler, apps)(users)).willReturn(users)
        given(mockDeveloperService.filterUsersBy(statusFilter)(users)).willReturn(users)
        given(mockDeveloperService.fetchDevelopers(eqTo(apps))(any[HeaderCarrier])).willReturn(Future.successful(developers))
      }

      def givenFetchDeveloperReturns(developer: ApplicationDeveloper) = {
        given(mockDeveloperService.fetchDeveloper(eqTo(developer.email))(any[HeaderCarrier])).willReturn(Future.successful(developer))
      }

      def givenDeleteDeveloperReturns(result: DeveloperDeleteResult) = {
        given(mockDeveloperService.deleteDeveloper(anyString, anyString)(any[HeaderCarrier])).willReturn(Future.successful(result))
      }
    }

    "developersPage" should {

      "default to page 1 with 100 items in table" in new Setup {
        givenASuccessfulLogin
        givenNoDataSuppliedDelegateServices
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        bodyOf(result) should include("data-page-length=\"100\"")
      }

      "go to loginpage with error if user is not authenticated" in new Setup {
        val loginDetails = LoginDetails("userName", Protected("password"))
        given(developersController.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))
        val result = await(developersController.developersPage(None, None)(aLoggedOutRequest))
        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }

      "load successfully if user is authenticated and authorised" in new Setup {
        givenASuccessfulLogin
        givenNoDataSuppliedDelegateServices
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        status(result) shouldBe 200
        bodyOf(result) should include("<h1>Developers</h1>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers\">Developers</a>")
      }


      "load successfully if user is authenticated and authorised, but not show dashboard tab if external test" in new Setup {
        givenASuccessfulLogin
        givenNoDataSuppliedDelegateServices
        given(developersController.appConfig.isExternalTestEnvironment).willReturn(true)
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        status(result) shouldBe 200
        bodyOf(result) should include("<h1>Developers</h1>")
        bodyOf(result) shouldNot include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/dashboard\">Dashboard</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        bodyOf(result) should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers\">Developers</a>")
      }

      "go to unauthorised page if user is not authorised" in new Setup {
        givenAUnsuccessfulLogin
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
      }

      "list all developers when filtering off" in new Setup {
        val users = Seq(
          User("sample@example.com", "Sample", "Email", Some(false)),
          User("another@example.com", "Sample2", "Email", Some(true)),
          User("someone@example.com", "Sample3", "Email", Some(true)))
        val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@example.com", CollaboratorRole.DEVELOPER))
        val applications = Seq(ApplicationResponse(UUID.randomUUID(), "application", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState()))
        val devs = users.map(Developer.createFromUser(_, applications))
        givenASuccessfulLogin
        givenDelegateServicesSupply(applications, devs, devs)
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        status(result) shouldBe 200
        collaborators.foreach(c => bodyOf(result) should include(c.emailAddress))
      }

      "display message if no developers found by filter" in new Setup {
        val collaborators = Set[Collaborator]()
        val applications = Seq(ApplicationResponse(UUID.randomUUID(), "application", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState()))
        givenASuccessfulLogin
        givenDelegateServicesSupply(applications, noUsers, noUsers)
        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))
        status(result) shouldBe 200
        bodyOf(result) should include("No developers for your selected filter")
      }
    }

    "deleteDeveloperPage" should {
      val emailAddress = "someone@example.com"
      val apps = Seq(anApplication(Set(Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR),
        Collaborator("someoneelse@example.com", CollaboratorRole.ADMINISTRATOR))))
      val developer = User(emailAddress, "Firstname", "Lastname", Some(true)).toDeveloper(apps)

      "not allow a non super user to access the page" in new Setup {
        givenASuccessfulLogin
        val result = await(developersController.deleteDeveloperPage(emailAddress)(aLoggedInRequest))
        status(result) shouldBe 401
      }

      "allow a super user to access the page" in new Setup {
        givenASuccessfulSuperUserLogin
        givenFetchDeveloperReturns(developer)
        val result = await(addToken(developersController.deleteDeveloperPage(emailAddress))(aSuperUserLoggedInRequest))
        status(result) shouldBe 200
        verify(mockDeveloperService).fetchDeveloper(eqTo(emailAddress))(any[HeaderCarrier])
      }
    }

    "deleteDeveloperAction" should {
      val emailAddress = "someone@example.com"
      val apps = Seq(anApplication(Set(Collaborator(emailAddress, CollaboratorRole.ADMINISTRATOR),
        Collaborator("someoneelse@example.com", CollaboratorRole.ADMINISTRATOR))))
      val developer = User(emailAddress, "Firstname", "Lastname", Some(true)).toDeveloper(apps)

      "not allow a non super user to access the page" in new Setup {
        givenASuccessfulLogin
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aLoggedInRequest))
        status(result) shouldBe 401
      }

      "allow a super user to access the page" in new Setup {
        givenASuccessfulSuperUserLogin
        givenDeleteDeveloperReturns(DeveloperDeleteSuccessResult)
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aSuperUserLoggedInRequest))
        status(result) shouldBe 200
        verify(mockDeveloperService).deleteDeveloper(eqTo(emailAddress), eqTo(superUserName))(any[HeaderCarrier])
      }

      "return an internal server error when the delete fails" in new Setup {
        givenASuccessfulSuperUserLogin
        givenDeleteDeveloperReturns(DeveloperDeleteFailureResult)
        val result = await(developersController.deleteDeveloperAction(emailAddress)(aSuperUserLoggedInRequest))
        status(result) shouldBe 500
      }
    }
  }
}
