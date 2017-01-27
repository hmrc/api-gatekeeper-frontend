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

import java.util.UUID

import connectors.AuthConnector.InvalidCredentials
import connectors.{ApiDefinitionConnector, ApplicationConnector, AuthConnector, DeveloperConnector}
import controllers.DevelopersController
import services.DeveloperService
import model.LoginDetails.{JsonStringDecryption, JsonStringEncryption}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.SignedTokenProvider
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.frontend.auth.AuthenticationProvider
import uk.gov.hmrc.play.http.{HeaderCarrier, SessionKeys}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future
import org.mockito.cglib.proxy.Proxy
import org.mockito.Mock
import play.api.mvc.Result
import play.api.mvc.Request

class DevelopersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  Helpers.running(fakeApplication) {

    trait Setup {
      
      val mockAuthConnector = mock[AuthConnector]
      val mockAuthProvider = mock[AuthenticationProvider]
      val mockApiDefinitionConnector = mock[ApiDefinitionConnector]
      val mockDeveloperService = mock[DeveloperService]
      
      val developersController = new DevelopersController {
        val authConnector = mockAuthConnector
        val authProvider = mockAuthProvider
        val apiDefinitionConnector = mockApiDefinitionConnector
        val developerService = mockDeveloperService
      }

      implicit val encryptedStringFormats = JsonStringEncryption
      implicit val decryptedStringFormats = JsonStringDecryption
      implicit val format = Json.format[LoginDetails]

      val userName = "userName"
      val authToken = SessionKeys.authToken -> "some-bearer-token"
      val userToken = GatekeeperSessionKeys.LoggedInUser -> "userName"
      val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
      val aLoggedOutRequest = FakeRequest().withSession()
      val noUsers = Seq.empty[User];
      
      def givenAUnsucessfulLogin(): Unit = {
        givenALogin(false)
      }

      def givenASucessfulLogin(): Unit = {
        givenALogin(true)
      }

      def givenALogin(sucessful: Boolean): Unit = {
        val successfulAuthentication = SuccessfulAuthentication(BearerToken("bearer-token", DateTime.now().plusMinutes(10)), userName, None)
        given(mockAuthConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.successful(successfulAuthentication))
        given(mockAuthConnector.authorized(any[Role])(any[HeaderCarrier])).willReturn(Future.successful(sucessful))
      }

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noUsers, noUsers)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], users: Seq[User], developers: Seq[User]): Unit = {
        val apiFiler = ApiFilter(None)
        val statusFilter = StatusFilter(None)
        given(mockDeveloperService.fetchApplications(org.mockito.Matchers.eq(apiFiler))(any[HeaderCarrier])).willReturn(Future.successful(apps))
        given(mockApiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockDeveloperService.filterUsersBy(apiFiler, apps)(users)).willReturn(users)
        given(mockDeveloperService.filterUsersBy(statusFilter)(users)).willReturn(users)
        given(mockDeveloperService.fetchDevelopers(any[HeaderCarrier])).willReturn(Future.successful(developers))
        given(mockDeveloperService.emailList(users)).willReturn("")
      }
    }
    
    "developersPage" should {

      "default to page 1 with 100 items in table" in new Setup {

        val overridenDevelopersController = new DevelopersController {
          val authConnector = mockAuthConnector
          val authProvider = mockAuthProvider
          val apiDefinitionConnector = mockApiDefinitionConnector
          val developerService = mockDeveloperService
          
          override def validPageResult(page: PageableCollection[User], emails: String, apis: Seq[APIDefinition], filter: Option[String], status: Option[String])(implicit request: Request[_]): Result = {
            page.pageNumber shouldBe 1
            page.pageSize shouldBe 100
            Ok
          }
        }

        givenASucessfulLogin
        givenNoDataSuppliedDelegateServices
        await(overridenDevelopersController.developersPage(None, None, None, None)(aLoggedInRequest))
      }

      "go to loginpage with error if user is not authenticated" in new Setup {
        
        val loginDetails = LoginDetails("userName", Protected("password"))
        given(developersController.authConnector.login(any[LoginDetails])(any[HeaderCarrier])).willReturn(Future.failed(new InvalidCredentials))

        val result = await(developersController.developersPage(None, None, Some(1), Some(10))(aLoggedOutRequest))

        redirectLocation(result) shouldBe Some("/api-gatekeeper/login")
      }


      "load successfully if user is authenticated and authorised" in new Setup {
        
        givenASucessfulLogin
        givenNoDataSuppliedDelegateServices

        val result = await(developersController.developersPage(None, None, Some(1), Some(10))(aLoggedInRequest))

        status(result) shouldBe 200
        bodyOf(result) should include("Dashboard")
      }


      "go to unauthorised page if user is not authorised" in new Setup {

        givenAUnsucessfulLogin

        val result = await(developersController.developersPage(None, None, Some(1), Some(10))(aLoggedInRequest))

        status(result) shouldBe 401
        bodyOf(result) should include("Only Authorised users can access the requested page")
      }


      "list all developers when filtering off" in new Setup {

        val users = Seq(
          User("sample@email.com", "Sample", "Email", Some(false)),
          User("another@email.com", "Sample2", "Email", Some(true)),
          User("someone@email.com", "Sample3", "Email", Some(true)))

        val collaborators = Set(Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))
        val applications = Seq(ApplicationResponse(UUID.randomUUID(), "application", None, collaborators, DateTime.now(), ApplicationState()))

        givenASucessfulLogin
        givenDelegateServicesSupply(applications, users, users);

        val result = await(developersController.developersPage(None, None, Some(1), Some(10))(aLoggedInRequest))

        status(result) shouldBe 200
        collaborators.foreach(c => bodyOf(result) should include(c.emailAddress))
      }

      "display message if no developers found by filter" in new Setup{

        val collaborators = Set[Collaborator]()
        val applications = Seq(ApplicationResponse(UUID.randomUUID(), "application", None, collaborators, DateTime.now(), ApplicationState()))

        givenASucessfulLogin
        givenDelegateServicesSupply(applications, noUsers, noUsers);
        
        val result = await(developersController.developersPage(None, None, Some(1), Some(10))(aLoggedInRequest))

        status(result) shouldBe 200
        bodyOf(result) should include("No developers for your selected filter")
      }
    }
  }
}
