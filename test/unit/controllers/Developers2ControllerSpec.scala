/*
 * Copyright 2019 HM Revenue & Customs
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

import controllers.{Developers2Controller, DevelopersController}
import model._
import org.joda.time.DateTime
import org.mockito.BDDMockito._
import org.mockito.Matchers.{any, anyString, eq => eqTo}
import org.mockito.Mockito.verify
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.utils.WithCSRFAddToken

import scala.concurrent.Future
import scala.concurrent.Future.{failed, successful}

class Developers2ControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  // Search by email
  // with submit
  // List of emails and other columns

  Helpers.running(fakeApplication) {

    def anApplication(collaborators: Set[Collaborator]) = {
      ApplicationResponse(UUID.randomUUID(), "clientid", "application", "PRODUCTION", None, collaborators, DateTime.now(), Standard(), ApplicationState())
    }

    trait Setup extends ControllerSetupBase {

      implicit val appConfig = mockConfig
      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      val loggedInSuperUser = "superUserName"
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken)
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken)
      given(appConfig.superUsers).willReturn(Seq(loggedInSuperUser))
      given(appConfig.strideLoginUrl).willReturn("https://loginUri")
      given(appConfig.appName).willReturn("Gatekeeper app name")
      given(appConfig.gatekeeperSuccessUrl).willReturn("successUrl_not_checked")

      val mockDeveloperService = mock[DeveloperService]

      val developersController = new Developers2Controller(mockAuthConnector, mockDeveloperService) {
        override val appConfig = mockConfig
      }

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(Seq.empty[ApplicationResponse], noDevs)
      }

      def givenDelegateServicesSupply(apps: Seq[ApplicationResponse], developers: Seq[ApplicationDeveloper]): Unit = {
        val apiFilter = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter = StatusFilter(None)
        val users = developers.map(developer => User(developer.email, developer.firstName, developer.lastName, developer.verified, developer.organisation))
        given(mockApplicationService.fetchApplications(eqTo(apiFilter), eqTo(environmentFilter))(any[HeaderCarrier])).willReturn(successful(apps))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockDeveloperService.filterUsersBy(apiFilter, apps)(developers)).willReturn(developers)
        given(mockDeveloperService.filterUsersBy(statusFilter)(developers)).willReturn(developers)
        given(mockDeveloperService.getDevelopersWithApps(eqTo(apps), eqTo(users))(any[HeaderCarrier])).willReturn(developers)
        given(mockDeveloperService.fetchUsers(any[HeaderCarrier])).willReturn(successful(users))
      }

      def givenFetchDeveloperReturns(developer: ApplicationDeveloper) = {
        given(mockDeveloperService.fetchDeveloper(eqTo(developer.email))(any[HeaderCarrier])).willReturn(successful(developer))
      }

      def givenDeleteDeveloperReturns(result: DeveloperDeleteResult) = {
        given(mockDeveloperService.deleteDeveloper(anyString, anyString)(any[HeaderCarrier])).willReturn(successful(result))
      }

      def givenRemoveMfaReturns(user: Future[User]): BDDMyOngoingStubbing[Future[User]] = {
        given(mockDeveloperService.removeMfa(anyString, anyString)(any[HeaderCarrier])).willReturn(user)
      }
    }

    "developersPage" should {
      "show no results when initially opened" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser
        givenNoDataSuppliedDelegateServices

        val result = await(developersController.developersPage()(aLoggedInRequest))

        bodyOf(result) should include("Developers New")

        verifyAuthConnectorCalledForUser
      }

      "allow searching by email" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser
        givenNoDataSuppliedDelegateServices

        private val emailAddress = "developer@example.com"

        // Note: Developers is both users and collaborators
        given(mockDeveloperService.searchDevelopers(any())).willReturn(List(emailAddress))

        val result = await(developersController.developersPage(Some(emailAddress))(aLoggedInRequest))

//        bodyOf(result) should include(emailAddress)

        verifyAuthConnectorCalledForUser

        verify(mockDeveloperService).searchDevelopers(emailAddress)
      }

      // TODO - Test partial email match
    }
  }
}