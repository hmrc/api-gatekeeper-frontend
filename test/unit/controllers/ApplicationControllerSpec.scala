/*
 * Copyright 2020 HM Revenue & Customs
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

import java.net.URLEncoder
import java.util.UUID

import controllers.ApplicationController
import model.Environment._
import model.RateLimitTier.RateLimitTier
import model._
import org.joda.time.DateTime
import org.jsoup.Jsoup
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => eqTo, _}
import org.mockito.Mockito.{never, times, verify}
import org.scalatest.mockito.MockitoSugar
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.{DeveloperService, SubscriptionFieldsService}
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import unit.utils.WithCSRFAddToken
import utils.CSRFTokenHelper._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.Future.successful

class ApplicationControllerSpec extends UnitSpec with MockitoSugar with WithFakeApplication with WithCSRFAddToken {

  implicit val materializer = fakeApplication.materializer

  running(fakeApplication) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> fakeApplication.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin()))), Seq.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), Seq.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), Seq.empty)

      val mockDeveloperService = mock[DeveloperService]
      val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

      val developers = List[User] {
        new User("joe.bloggs@example.co.uk", "joe", "bloggs", None, None, false)
      }

      val underTest = new ApplicationController(
        mockApplicationService,
        mockApiDefinitionService,
        mockDeveloperService,
        mockSubscriptionFieldsService,
        mockAuthConnector
      )(mockConfig, global)

      given(mockConfig.superUsers).willReturn(Seq("superUserName"))

      given(mockConfig.superUserRole).willReturn(superUserRole)
      given(mockConfig.adminRole).willReturn(adminRole)
      given(mockConfig.userRole).willReturn(userRole)

      given(mockConfig.gatekeeperSuccessUrl).willReturn("http://mock-gatekeeper-frontend/api-gatekeeper/applications")

      given(mockConfig.strideLoginUrl).willReturn("https://loginUri")
      given(mockConfig.appName).willReturn("Gatekeeper app name")


      def givenThePaginatedApplicationsWillBeReturned = {
        val allSubscribedApplications: PaginatedSubscribedApplicationResponse = aPaginatedSubscribedApplicationResponse(Seq.empty)
        given(mockApplicationService.searchApplications(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

      }
    }

    "applicationsPage" should {

      "on request with no specified environment all sandbox applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Applications"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Applications</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")

        verifyAuthConnectorCalledForUser

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), any[Map[String, String]])(any[HeaderCarrier])
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(any[HeaderCarrier])
      }

      "on request for production all production applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("PRODUCTION"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(PRODUCTION)), any[Map[String, String]])(any[HeaderCarrier])
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(PRODUCTION)))(any[HeaderCarrier])
      }

      "on request for sandbox all sandbox applications supplied" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val eventualResult: Future[Result] = underTest.applicationsPage(environment = Some("SANDBOX"))(aLoggedInRequest)

        status(eventualResult) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), any[Map[String, String]])(any[HeaderCarrier])
        verify(mockApiDefinitionService).fetchAllApiDefinitions(eqTo(Some(SANDBOX)))(any[HeaderCarrier])
      }

      "pass requested params with default params and default environment of SANDBOX to the service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenThePaginatedApplicationsWillBeReturned

        val aLoggedInRequestWithParams = aLoggedInRequest.copyFakeRequest(
          uri = "/applications?search=abc&apiSubscription=ANY&status=CREATED&termsOfUse=ACCEPTED&accessType=STANDARD")
        val expectedParams = Map(
          "page" -> "1",
          "pageSize" -> "100",
          "sort" -> "NAME_ASC",
          "search" -> "abc",
          "apiSubscription" -> "ANY",
          "status" -> "CREATED",
          "termsOfUse" -> "ACCEPTED",
          "accessType" -> "STANDARD")
        val result = await(underTest.applicationsPage()(aLoggedInRequestWithParams))

        status(result) shouldBe OK

        verify(mockApplicationService).searchApplications(eqTo(Some(SANDBOX)), eqTo(expectedParams))(any[HeaderCarrier])
      }

      "redirect to the login page if the user is not logged in" in new Setup {
        givenAUnsuccessfulLogin()

        val result = await(underTest.applicationsPage()(aLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe
          Some(
            s"https://loginUri?successURL=${URLEncoder.encode("http://mock-gatekeeper-frontend/api-gatekeeper/applications", "UTF-8")}" +
              s"&origin=${URLEncoder.encode("Gatekeeper app name", "UTF-8")}")
      }

      "show button to add Privileged or ROPC app to superuser" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        val allSubscribedApplications: PaginatedSubscribedApplicationResponse = aPaginatedSubscribedApplicationResponse(Seq.empty)
        given(mockApplicationService.searchApplications(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

        val result = await(underTest.applicationsPage()(aSuperUserLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body should include("Add privileged or ROPC application")

        verifyAuthConnectorCalledForUser
      }

      "not show button to add Privileged or ROPC app to non-superuser" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val allSubscribedApplications: PaginatedSubscribedApplicationResponse = aPaginatedSubscribedApplicationResponse(Seq.empty)
        given(mockApplicationService.searchApplications(any(), any())(any[HeaderCarrier])).willReturn(Future.successful(allSubscribedApplications))
        given(mockApiDefinitionService.fetchAllApiDefinitions(any())(any[HeaderCarrier])).willReturn(Seq.empty[APIDefinition])
        given(mockConfig.title).willReturn("Unit Test Title")

        val result = await(underTest.applicationsPage()(aLoggedInRequest))
        status(result) shouldBe OK

        val body = bodyOf(result)

        body shouldNot include("Add privileged or ROPC application")

        verifyAuthConnectorCalledForUser
      }
    }

    "resendVerification" should {
      "call backend with correct application id and gatekeeper id when resend verification is invoked" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(mockApplicationService.resendVerification(appCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
          .willReturn(Future.successful(ResendVerificationSuccessful))

        val result = await(underTest.resendVerification(applicationId)(aLoggedInRequest))

        appCaptor.getValue shouldBe basicApplication
        gatekeeperIdCaptor.getValue shouldBe userName
        verifyAuthConnectorCalledForUser
      }
    }

    "manageScopes" should {
      "fetch an app with Privileged access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(privilegedApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "fetch an app with ROPC access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(ropcApplication)

        val result = await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "return an error for a Standard app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        intercept[RuntimeException] {
          await(addToken(underTest.manageScopes(applicationId))(aSuperUserLoggedInRequest))
        }
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageScopes(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateScopes" should {
      "call the service to update scopes when a valid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateScopesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello, individual-benefits")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")

        verify(mockApplicationService)
          .updateScopes(eqTo(application.application), eqTo(Set("hello", "individual-benefits")))(any[HeaderCarrier])

        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when the service indicates that the scopes are invalid" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateScopesInvalidScopesResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("scopes" -> "hello")
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe BAD_REQUEST
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody()
        val result = await(addToken(underTest.updateScopes(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateScopes(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier])
      }
    }

    "manageWhitelistedIp" should {
      "return the manage whitelisted IP page for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(anAdminLoggedInRequest))

        status(result) shouldBe OK
        bodyOf(result) should include("Manage whitelisted IP")
      }

      "return the manage whitelisted IP page for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        bodyOf(result) should include("Manage whitelisted IP")
      }

      "return the forbidden page for a normal user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(underTest.manageWhitelistedIpPage(applicationId)(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        bodyOf(result) should include("You do not have permission")
      }
    }

    "manageWhitelistedIpAction" should {
      val whitelistedIpToUpdate: String = "1.1.1.0/24"

      "manage whitelisted IP using the app service for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()
        given(mockApplicationService.manageWhitelistedIp(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))
        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockApplicationService).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(any[HeaderCarrier])
      }

      "manage whitelisted IP using the app service for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()
        given(mockApplicationService.manageWhitelistedIp(any[ApplicationResponse], any[Set[String]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateIpWhitelistSuccessResult))
        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")
        verify(mockApplicationService).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(any[HeaderCarrier])
      }

      "return bad request for invalid values" in new Setup {
        val invalidWhitelistedIps: Seq[String] = Seq(
          "1.1.1.0", // no mask
          "1.1.1.0/33", // mask greater than 32
          "1.1.1.0/23", // mask less than 24
          "1.1.1.0/", // incomplete mask
          "1.1.1/24", // IP address missing one octet
          "10.1.1.0/24", // within a private network range
          "172.20.1.0/24", // within a private network range
          "192.168.1.0/24", // within a private network range
          "10.0.0.0/24", // within a private network range, using the network address
          "10.255.255.255/24" // within a private network range, using the broadcast address
        )

        invalidWhitelistedIps.foreach { invalidWhitelistedIp =>
          givenTheUserIsAuthorisedAndIsASuperUser()
          givenTheAppWillBeReturned()
          val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> invalidWhitelistedIp)

          val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

          status(result) shouldBe BAD_REQUEST
          verify(mockApplicationService, times(0)).manageWhitelistedIp(eqTo(application.application), eqTo(Set(whitelistedIpToUpdate)))(any[HeaderCarrier])
        }
      }

      "return the forbidden page for a normal user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()
        val request = aLoggedInRequest.withFormUrlEncodedBody("whitelistedIps" -> whitelistedIpToUpdate)

        val result = await(underTest.manageWhitelistedIpAction(applicationId)(request))

        status(result) shouldBe FORBIDDEN
        bodyOf(result) should include("You do not have permission")
      }
    }

    "manageOverrides" should {
      "fetch an app with Standard access for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe OK
        verifyAuthConnectorCalledForSuperUser
      }

      "return an error for a ROPC app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(ropcApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return an error for a Privileged app" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(privilegedApplication)

        intercept[RuntimeException] {
          await(addToken(underTest.manageAccessOverrides(applicationId))(aSuperUserLoggedInRequest))
        }
      }

      "return forbidden for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageAccessOverrides(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
      }
    }

    "updateOverrides" should {
      "call the service to update overrides when a valid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier]))
          .willReturn(Future.successful(UpdateOverridesSuccessResult))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "hello, individual-benefits",
          "suppressIvForAgentsEnabled" -> "true", "suppressIvForAgentsScopes" -> "openid, email",
          "suppressIvForOrganisationsEnabled" -> "true", "suppressIvForOrganisationsScopes" -> "address, openid:mdtp",
          "suppressIvForIndividualsEnabled" -> "true", "suppressIvForIndividualsScopes" -> "email, openid:hmrc-enrolments")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")

        verify(mockApplicationService).updateOverrides(
          eqTo(application.application),
          eqTo(Set(
            PersistLogin(),
            GrantWithoutConsent(Set("hello", "individual-benefits")),
            SuppressIvForAgents(Set("openid", "email")),
            SuppressIvForOrganisations(Set("address", "openid:mdtp")),
            SuppressIvForIndividuals(Set("email", "openid:hmrc-enrolments"))
          )))(any[HeaderCarrier])

        verifyAuthConnectorCalledForSuperUser
      }

      "return a bad request when an invalid form is submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
          "persistLoginEnabled" -> "true",
          "grantWithoutConsentEnabled" -> "true", "grantWithoutConsentScopes" -> "")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier])

        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when a form is submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("persistLoginEnabled" -> "true")

        val result = await(addToken(underTest.updateAccessOverrides(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateOverrides(any[ApplicationResponse], any[Set[OverrideFlag]])(any[HeaderCarrier])
      }
    }

    "subscribeToApi" should {
      "call the service to subscribe to the API when submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.subscribeToApi(any[Application], anyString, anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.subscribeToApi(applicationId, "hello", "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/subscriptions")

        verify(mockApplicationService).subscribeToApi(eqTo(basicApplication), eqTo("hello"), eqTo("1.0"))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.subscribeToApi(applicationId, "hello", "1.0"))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).subscribeToApi(eqTo(basicApplication), anyString, anyString)(any[HeaderCarrier])
      }
    }

    "unsubscribeFromApi" should {
      "call the service to unsubscribe from the API when submitted for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()

        given(mockApplicationService.unsubscribeFromApi(any[Application], anyString, anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, "hello", "1.0"))(aSuperUserLoggedInRequest))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/subscriptions")

        verify(mockApplicationService).unsubscribeFromApi(eqTo(basicApplication), eqTo("hello"), eqTo("1.0"))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }

      "return forbidden when submitted for a non-super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val result = await(addToken(underTest.unsubscribeFromApi(applicationId, "hello", "1.0"))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unsubscribeFromApi(any[Application], anyString, anyString)(any[HeaderCarrier])
      }
    }

    "updateSubscriptionFields" should {
      val context = "hello"
      val version = "1.0"

      val validForm = Seq(
        "fields[0].name" -> "field1",
        "fields[0].value" -> "value1",
        "fields[0].description" -> "desc1",
        "fields[0].hint" -> "hint1",
        "fields[0].type" -> "STRING",
        "fields[1].name" -> "field2",
        "fields[1].value" -> "value2",
        "fields[1].description" -> "desc0",
        "fields[1].hint" -> "hint0",
        "fields[1].type" -> "STRING"
      )

      "save subscription field values" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned()
        given(mockSubscriptionFieldsService.saveFieldValues(any[Application], any[String], any[String], any[ApiSubscriptionFields.Fields])(any[HeaderCarrier]))
          .willReturn(successful(HttpResponse(OK)))

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(validForm: _*)

        val result = await(addToken(underTest.updateSubscriptionFields(applicationId, context, version))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/subscriptions")

        verify(mockSubscriptionFieldsService).saveFieldValues(
          eqTo(application.application),
          eqTo(context),
          eqTo(version),
          eqTo(Map("field1" -> "value1", "field2" -> "value2")))(any[HeaderCarrier])
        verifyAuthConnectorCalledForSuperUser
      }
    }

    "manageRateLimitTier" should {
      "fetch the app and return the page for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())

        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden for a super user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())

      }

      "return forbidden for a user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.manageRateLimitTier(applicationId))(aLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
      }
    }

    "updateRateLimitTier" should {
      "call the service to update the rate limit tier when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.updateRateLimitTier(any[Application], any[RateLimitTier])(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe SEE_OTHER
        redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId")

        verify(mockApplicationService).updateRateLimitTier(eqTo(basicApplication), eqTo(RateLimitTier.GOLD))(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).updateRateLimitTier(any[Application], any[RateLimitTier])(any[HeaderCarrier])
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aLoggedInRequest.withFormUrlEncodedBody("tier" -> "GOLD")

        val result = await(addToken(underTest.updateRateLimitTier(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).updateRateLimitTier(any[Application], any[RateLimitTier])(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
      }
    }

    "handleUplift" should {

      "call backend with correct application id and gatekeeper id when application is approved" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])
        given(mockApplicationService.approveUplift(appCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier]))
          .willReturn(Future.successful(ApproveUpliftSuccessful))
        val result = await(underTest.handleUplift(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("action", "APPROVE"))))
        appCaptor.getValue shouldBe basicApplication
        gatekeeperIdCaptor.getValue shouldBe userName

        verifyAuthConnectorCalledForUser
      }
    }

    "handleUpdateRateLimitTier" should {
      val tier = RateLimitTier.GOLD

      "change the rate limit for a super user" in new Setup {
        givenTheUserIsAuthorisedAndIsASuperUser()
        givenTheAppWillBeReturned(application)

        val appCaptor = ArgumentCaptor.forClass(classOf[Application])
        val newTierCaptor = ArgumentCaptor.forClass(classOf[RateLimitTier])
        val hcCaptor = ArgumentCaptor.forClass(classOf[HeaderCarrier])

        given(mockApplicationService.updateRateLimitTier(appCaptor.capture(), newTierCaptor.capture())(hcCaptor.capture()))
          .willReturn(Future.successful(ApplicationUpdateSuccessResult))

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", tier.toString))))
        status(result) shouldBe SEE_OTHER

        appCaptor.getValue shouldBe basicApplication
        newTierCaptor.getValue shouldBe tier

        verify(mockApplicationService, times(1)).updateRateLimitTier(basicApplication, tier)(hcCaptor.getValue)

        verifyAuthConnectorCalledForUser
      }

      "not call the application connector for a normal user " in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned(application)

        val result = await(underTest.handleUpdateRateLimitTier(applicationId)(aLoggedInRequest.withFormUrlEncodedBody(("tier", "GOLD"))))
        status(result) shouldBe SEE_OTHER

        verify(mockApplicationService, never()).updateRateLimitTier(any[Application], any[RateLimitTier])(any[HeaderCarrier])

        verifyAuthConnectorCalledForUser
      }
    }

    "createPrivOrROPCApp" should {

      val appId = "123456789"
      val appName = "My New App"
      val privilegedAccessType = AccessType.PRIVILEGED
      val ropcAccessType = AccessType.ROPC
      val description = "An application description"
      val adminEmail = "emailAddress@example.com"
      val clientId = "This-isac-lient-ID"
      val clientSecret = "THISISACLIENTSECRET"
      val totpSecret = "THISISATOTPSECRETFORPRODUCTION"
      val totp = Some(TotpSecrets(totpSecret, "THISISNOTUSED"))
      val privAccess = AppAccess(AccessType.PRIVILEGED, Seq())
      val ropcAccess = AppAccess(AccessType.ROPC, Seq())

      "with invalid form fields" can {
        "show the correct error message when no environment is chosen" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", ""),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what environment")
        }

        "show the correct error message when no access type is chosen" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", ""),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Tell us what access type")
        }

        "show the correct error message when the app name is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", ""),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name")

        }

        "show the correct error message when the new prod app name already exists in prod" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            UUID.randomUUID(), "clientid1", "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application name that does not already exist")
        }

        "allow creation of a sandbox app if name already exists in production" in new Setup {

          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            UUID.randomUUID(), "clientid1", "gatewayId", "I Already Exist", "PRODUCTION", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockConfig.isExternalTestEnvironment).willReturn(true)
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "allow creation of a sandbox app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            UUID.randomUUID(), "clientid1", "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockConfig.isExternalTestEnvironment).willReturn(true)
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "SANDBOX", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.SANDBOX.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "allow creation of a prod app if name already exists in sandbox" in new Setup {
          val collaborators = Set(Collaborator("sample@example.com", CollaboratorRole.ADMINISTRATOR))
          val existingApp = ApplicationResponse(
            UUID.randomUUID(), "clientid1", "gatewayId", "I Already Exist", "SANDBOX", None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())

          givenTheUserIsAuthorisedAndIsASuperUser()
          given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq(existingApp)))
          given(mockApplicationService
            .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
            .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, "I Already Exist", "PRODUCTION", clientId, totp, privAccess)))

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", "I Already Exist"),
              ("applicationDescription", description),
              ("adminEmail", adminEmail))))

          status(result) shouldBe OK

          bodyOf(result) should include("Application added")
          verifyAuthConnectorCalledForSuperUser
        }

        "show the correct error message when app description is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", ""),
              ("adminEmail", adminEmail))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an application description")
        }

        "show the correct error message when admin email is left empty" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", ""))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide an email address")
        }

        "show the correct error message when admin email is invalid" in new Setup {
          givenTheUserIsAuthorisedAndIsASuperUser()

          val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
            aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("environment", Environment.PRODUCTION.toString),
              ("accessType", privilegedAccessType.toString),
              ("applicationName", appName),
              ("applicationDescription", description),
              ("adminEmail", "notAValidEmailAddress"))))

          status(result) shouldBe BAD_REQUEST

          assertIncludesOneError(result, "Provide a valid email address")
        }
      }

      "with valid form fields" can {
        "but the user is not a superuser" should {
          "show 403 forbidden" in new Setup {
            givenTheUserHasInsufficientEnrolments()

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe FORBIDDEN
          }
        }

        "and the user is a superuser" should {
          "show the success page for a priv app in production" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "PRODUCTION", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include("This is your only chance to copy and save this application's TOTP.")
            bodyOf(result) should include(appId)
            bodyOf(result) should include("Production")
            bodyOf(result) should include("Privileged")
            bodyOf(result) should include(totpSecret)
            bodyOf(result) should include(clientId)
            verifyAuthConnectorCalledForSuperUser

          }

          "show the success page for a priv app in sandbox" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "SANDBOX", clientId, totp, privAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", privilegedAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include("This is your only chance to copy and save this application's TOTP.")
            bodyOf(result) should include(appId)
            bodyOf(result) should include("Sandbox")
            bodyOf(result) should include("Privileged")
            bodyOf(result) should include(totpSecret)
            bodyOf(result) should include(clientId)
            verifyAuthConnectorCalledForSuperUser
          }

          "show the success page for an ROPC app in production" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "PRODUCTION", clientId, None, ropcAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.PRODUCTION.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include(appId)
            bodyOf(result) should include("Production")
            bodyOf(result) should include("ROPC")
            bodyOf(result) should include(clientId)
            verifyAuthConnectorCalledForSuperUser
          }

          "show the success page for an ROPC app in sandbox" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            given(mockApplicationService.fetchApplications(any[HeaderCarrier])).willReturn(Future.successful(Seq()))
            given(mockApplicationService
              .createPrivOrROPCApp(any[Environment], any[String], any[String], any[Seq[Collaborator]], any[AppAccess])(any[HeaderCarrier]))
              .willReturn(Future.successful(CreatePrivOrROPCAppSuccessResult(appId, appName, "SANDBOX", clientId, None, ropcAccess)))

            val result = await(addToken(underTest.createPrivOrROPCApplicationAction())(
              aSuperUserLoggedInRequest.withFormUrlEncodedBody(
                ("environment", Environment.SANDBOX.toString),
                ("accessType", ropcAccessType.toString),
                ("applicationName", appName),
                ("applicationDescription", description),
                ("adminEmail", "a@example.com"))))

            status(result) shouldBe OK

            bodyOf(result) should include(appName)
            bodyOf(result) should include("Application added")
            bodyOf(result) should include(appId)
            bodyOf(result) should include("Sandbox")
            bodyOf(result) should include("ROPC")
            bodyOf(result) should include(clientId)
            verifyAuthConnectorCalledForSuperUser

          }
        }
      }
    }

    "manageSubscription" when {
      "the user is a superuser" should {
        "fetch the subscriptions with the fields" in new Setup {

          val subscription = Subscription("name", "serviceName", "context", Seq())
          givenTheUserIsAuthorisedAndIsASuperUser()
          givenTheAppWillBeReturned()
          given(mockApplicationService.fetchApplicationSubscriptions(any[Application], any[Boolean])(any[HeaderCarrier])).willReturn(Seq(subscription))

          val result = await(addToken(underTest.manageSubscription(applicationId))(aSuperUserLoggedInRequest))

          status(result) shouldBe OK
          verify(mockApplicationService, times(1)).fetchApplicationSubscriptions(eqTo(application.application), eqTo(true))(any[HeaderCarrier])
          verifyAuthConnectorCalledForSuperUser
        }
      }

      "the user is not a superuser" should {
        "show 403 forbidden" in new Setup {
          val subscription = Subscription("name", "serviceName", "context", Seq())

          givenTheUserHasInsufficientEnrolments()

          given(mockApplicationService.fetchApplicationSubscriptions(any[Application], any[Boolean])(any[HeaderCarrier])).willReturn(Seq(subscription))

          val result = await(addToken(underTest.manageSubscription(applicationId))(aLoggedInRequest))

          status(result) shouldBe FORBIDDEN
        }
      }
    }

    "applicationPage" should {
      "return the application details without subscription fields" in new Setup {
        val subscriptions = Seq(Subscription("name", "serviceName", "context", Seq()))

        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()
        given(mockApplicationService.fetchApplicationSubscriptions(any[Application], any[Boolean])(any[HeaderCarrier])).willReturn(subscriptions)
        given(mockDeveloperService.fetchDevelopersByEmails(eqTo(application.application.collaborators.map(colab => colab.emailAddress)))(any[HeaderCarrier]))
          .willReturn(developers)

        val result = await(addToken(underTest.applicationPage(applicationId))(aLoggedInRequest))

        status(result) shouldBe OK
        verify(mockApplicationService, times(1)).fetchApplicationSubscriptions(eqTo(application.application), eqTo(false))(any[HeaderCarrier])
        verify(mockSubscriptionFieldsService, never).fetchFields(any[Application], anyString, anyString)(any[HeaderCarrier])
        verifyAuthConnectorCalledForUser
      }

      "return the application details when the subscription service fails" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenTheAppWillBeReturned()
        given(mockApplicationService.fetchApplicationSubscriptions(any[Application], any[Boolean])(any[HeaderCarrier]))
          .willReturn(Future.failed(new FetchApplicationSubscriptionsFailed))
        given(mockDeveloperService.fetchDevelopersByEmails(eqTo(application.application.collaborators.map(colab => colab.emailAddress)))(any[HeaderCarrier]))
          .willReturn(developers)

        val result = await(addToken(underTest.applicationPage(applicationId))(aLoggedInRequest))

        status(result) shouldBe OK
        verify(mockApplicationService, times(1)).fetchApplicationSubscriptions(eqTo(application.application), eqTo(false))(any[HeaderCarrier])
        verifyAuthConnectorCalledForUser
      }
    }

    "manageTeamMembers" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            // The auth connector checks you are logged on. And the controller checks you are also a super user as it's a privileged app.
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "addTeamMember" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "addTeamMemberAction" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" should {
          val role = "DEVELOPER"

          "call the service to add the team member" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(any[Application], any[Collaborator], anyString)(any[HeaderCarrier]))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            verify(mockApplicationService)
              .addTeamMember(eqTo(application.application), eqTo(Collaborator(email, CollaboratorRole.DEVELOPER)), eqTo("superUserName"))(any[HeaderCarrier])
            verifyAuthConnectorCalledForUser
          }

          "redirect back to manageTeamMembers when the service call is successful" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(any[Application], any[Collaborator], anyString)(any[HeaderCarrier]))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/team-members")
            verifyAuthConnectorCalledForUser
          }

          "show 400 BadRequest when the service call fails with TeamMemberAlreadyExists" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(any[Application], any[Collaborator], anyString)(any[HeaderCarrier]))
              .willReturn(Future.failed(new TeamMemberAlreadyExists))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }

        "the form is invalid" should {
          "show 400 BadRequest when the email is invalid" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", "NOT AN EMAIL ADDRESS"),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }

          "show 400 BadRequest when the role is invalid" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", ""))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "manging a privileged app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 303 See Other when valid" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(any[Application], any[Collaborator], anyString)(any[HeaderCarrier]))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "removeTeamMember" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" should {
          "show the remove team member page successfully with the provided email address" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe OK
            bodyOf(result) should include(email)
            verifyAuthConnectorCalledForUser
          }
        }

        "the form is invalid" should {
          "show a 400 Bad Request" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "managing a privileged app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 200 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }
      }
    }

    "removeTeamMemberAction" when {
      val email = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" when {
          "the action is not confirmed" should {
            val confirm = "No"

            "redirect back to the manageTeamMembers page" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/team-members")
              verifyAuthConnectorCalledForUser
            }
          }

          "the action is confirmed" should {
            val confirm = "Yes"

            "call the service with the correct params" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(any[Application], anyString, anyString)(any[HeaderCarrier]))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER

              verify(mockApplicationService).removeTeamMember(eqTo(application.application), eqTo(email), eqTo("superUserName"))(any[HeaderCarrier])
              verifyAuthConnectorCalledForUser
            }

            "show a 400 Bad Request when the service fails with TeamMemberLastAdmin" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(any[Application], anyString, anyString)(any[HeaderCarrier]))
                .willReturn(Future.failed(new TeamMemberLastAdmin))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe BAD_REQUEST
            }

            "redirect to the manageTeamMembers page when the service call is successful" in new Setup {
              givenTheUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(any[Application], anyString, anyString)(any[HeaderCarrier]))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/$applicationId/team-members")
              verifyAuthConnectorCalledForUser
            }
          }
        }

        "the form is invalid" should {
          "show 400 Bad Request" in new Setup {
            givenTheUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "when managing a privileged app" should {
          "show 403 forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing a standard app" should {
          "show 303 OK" in new Setup {
            givenTheUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()
            given(mockApplicationService.removeTeamMember(any[Application], anyString, anyString)(any[HeaderCarrier]))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }

    "blockApplicationPage" should {

      "return the page for block app if admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.blockApplicationPage(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
        verifyAuthConnectorCalledForAdmin
      }


      "return forbidden for a non-admin" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.blockApplicationPage(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())

      }
    }

    "blockApplicationAction" should {
      "call the service to block application when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.blockApplication(any[Application], anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationBlockSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe OK

        verify(mockApplicationService).blockApplication(eqTo(basicApplication), any())(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).blockApplication(any(), any())(any[HeaderCarrier])
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.blockApplicationAction(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).blockApplication(any(), any())(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
      }

    }

    "unblockApplicationPage" should {

      "return the page for unblock app if admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.unblockApplicationPage(applicationId))(anAdminLoggedInRequest))

        status(result) shouldBe OK
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
        verifyAuthConnectorCalledForAdmin
      }


      "return forbidden for a non-admin" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned(application)

        val result = await(addToken(underTest.unblockApplicationPage(applicationId))(aSuperUserLoggedInRequest))

        status(result) shouldBe FORBIDDEN
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())

      }
    }

    "unblockApplicationAction" should {
      "call the service to unblock application when a valid form is submitted for an admin" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        given(mockApplicationService.unblockApplication(any[Application], anyString)(any[HeaderCarrier]))
          .willReturn(Future.successful(ApplicationUnblockSuccessResult))

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe OK

        verify(mockApplicationService).unblockApplication(eqTo(basicApplication), any())(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
        verifyAuthConnectorCalledForAdmin
      }

      "return a bad request when an invalid form is submitted for an admin user" in new Setup {
        givenTheUserIsAuthorisedAndIsAnAdmin()
        givenTheAppWillBeReturned()

        val request = anAdminLoggedInRequest.withFormUrlEncodedBody()

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe BAD_REQUEST

        verify(mockApplicationService, never).unblockApplication(any(), any())(any[HeaderCarrier])
        verifyAuthConnectorCalledForAdmin
      }

      "return forbidden when a form is submitted for a non-admin user" in new Setup {
        givenTheUserHasInsufficientEnrolments()
        givenTheAppWillBeReturned()

        val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody("applicationNameConfirmation" -> application.application.name)

        val result = await(addToken(underTest.unblockApplicationAction(applicationId))(request))

        status(result) shouldBe FORBIDDEN

        verify(mockApplicationService, never).unblockApplication(any(), any())(any[HeaderCarrier])
        verify(underTest.authConnector).authorise(eqTo(Enrolment(adminRole)), any[Retrieval[Any]])(any(), any())
      }

    }

    def titleOf(result: Result) = {
      val titleRegEx = """<title[^>]*>(.*)</title>""".r
      val title = titleRegEx.findFirstMatchIn(bodyOf(result)).map(_.group(1))
      title.isDefined shouldBe true
      title.get
    }

    def assertIncludesOneError(result: Result, message: String) = {

      val body = bodyOf(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("form-field--error").size == 1)
    }

    def aPaginatedSubscribedApplicationResponse(applications: Seq[SubscribedApplicationResponse]): PaginatedSubscribedApplicationResponse = {
      val page = 1
      val pageSize = 10
      PaginatedSubscribedApplicationResponse(applications, page, pageSize, total = applications.size, matching = applications.size)
    }
  }
}
