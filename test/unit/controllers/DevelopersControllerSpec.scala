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
import controllers.DevelopersController
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers
import org.mockito.Matchers._
import org.scalatest.mock.MockitoSugar
import play.api.test.Helpers
import play.api.test.Helpers._
import services.{ApplicationService, DeveloperService}
import uk.gov.hmrc.crypto.Protected
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.Future

class DevelopersControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication {

  implicit val materializer = fakeApplication.materializer

  Helpers.running(fakeApplication) {

    trait Setup extends ControllerSetupBase {

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new DevelopersController {
        val authConnector = mockAuthConnector
        val authProvider = mockAuthProvider
        val apiDefinitionConnector = mockApiDefinitionConnector
        val developerService = mockDeveloperService
        val applicationService = mockApplicationService
      }

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noUsers, noUsers)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], users: Seq[ApplicationDeveloper], developers: Seq[ApplicationDeveloper]): Unit = {
        val apiFiler = ApiFilter(None)
        val statusFilter = StatusFilter(None)
        given(mockApplicationService.fetchApplications(org.mockito.Matchers.eq(apiFiler))(any[HeaderCarrier])).willReturn(Future.successful(apps))
        given(mockApiDefinitionConnector.fetchAll()(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockDeveloperService.filterUsersBy(apiFiler, apps)(users)).willReturn(users)
        given(mockDeveloperService.filterUsersBy(statusFilter)(users)).willReturn(users)
        given(mockDeveloperService.fetchDevelopers(Matchers.eq(apps))(any[HeaderCarrier])).willReturn(Future.successful(developers))
      }
    }

    "developersPage" should {

      "default to page 1 with 100 items in table" in new Setup {

        val overridenDevelopersController = new DevelopersController {
          val authConnector = mockAuthConnector
          val authProvider = mockAuthProvider
          val apiDefinitionConnector = mockApiDefinitionConnector
          val developerService = mockDeveloperService
          val applicationService: ApplicationService = mockApplicationService
        }

        givenASuccessfulLogin
        givenNoDataSuppliedDelegateServices
        await(overridenDevelopersController.developersPage(None, None)(aLoggedInRequest))
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
        bodyOf(result) should include("Dashboard")
      }


      "go to unauthorised page if user is not authorised" in new Setup {

        givenAUnsuccessfulLogin

        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))

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

        val devs = users.map(Developer.createFromUser(_, applications))

        givenASuccessfulLogin
        givenDelegateServicesSupply(applications, devs, devs)

        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))

        status(result) shouldBe 200
        collaborators.foreach(c => bodyOf(result) should include(c.emailAddress))
      }

      "display message if no developers found by filter" in new Setup {

        val collaborators = Set[Collaborator]()
        val applications = Seq(ApplicationResponse(UUID.randomUUID(), "application", None, collaborators, DateTime.now(), ApplicationState()))

        givenASuccessfulLogin
        givenDelegateServicesSupply(applications, noUsers, noUsers)

        val result = await(developersController.developersPage(None, None)(aLoggedInRequest))

        status(result) shouldBe 200
        bodyOf(result) should include("No developers for your selected filter")
      }
    }
  }
}
