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

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

import org.apache.pekko.stream.Materializer

import play.api.mvc.AnyContentAsFormUrlEncoded
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.apiplatform.modules.tpd.emailpreferences.domain.models.EmailTopic.{BUSINESS_AND_POLICY, EVENT_INVITES, RELEASE_SCHEDULES, TECHNICAL}
import uk.gov.hmrc.apiplatform.modules.tpd.emailpreferences.domain.models.{EmailPreferences, TaxRegimeInterests}
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.domain.models._
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.xml.{Collaborator, OrganisationId, VendorId, XmlOrganisation}
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.views.html.developers._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class DevelopersControllerSpec extends ControllerBaseSpec {

  implicit val materializer: Materializer           = app.materializer
  private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView                    = app.injector.instanceOf[ForbiddenView]
  private lazy val developersView                   = app.injector.instanceOf[DevelopersView]
  private lazy val removeEmailPref                  = app.injector.instanceOf[RemoveEmailPreferences]

  Helpers.running(app) {

    val apiVersion1 = ApiVersionNbr("1.0")
    val apiVersion2 = ApiVersionNbr("2.0")

    trait Setup extends ControllerSetupBase {

      override val aLoggedInRequest          = FakeRequest().withSession(authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken).withCSRFToken

      val developersController = new DevelopersController(
        forbiddenView,
        mockDeveloperService,
        mockApiDefinitionService,
        mockXmlService,
        mcc,
        developersView,
        removeEmailPref,
        errorTemplateView,
        StrideAuthorisationServiceMock.aMock,
        LdapAuthorisationServiceMock.aMock
      )

      def givenNoDataSuppliedDelegateServices(): Unit = {
        givenDelegateServicesSupply(List.empty[ApplicationWithCollaborators], noDevs)
      }

      def givenDelegateServicesSupply(apps: List[ApplicationWithCollaborators], developers: List[Developer]): Unit = {
        val apiFilter         = ApiFilter(Some(""))
        val environmentFilter = ApiSubscriptionInEnvironmentFilter(Some(""))
        val statusFilter      = StatusFilter(None)
        val users             = developers.map(developer => RegisteredUser(developer.email, UserId.random, developer.firstName, developer.lastName, developer.verified))
        ApplicationServiceMock.FetchApplications.returnsFor(apiFilter, environmentFilter, apps: _*)
        FetchAllApiDefinitions.inAny.returns()
        DeveloperServiceMock.FilterUsersBy.returnsFor(apiFilter, apps: _*)(developers: _*)
        DeveloperServiceMock.FilterUsersBy.returnsFor(statusFilter)(developers: _*)
        DeveloperServiceMock.GetDevelopersWithApps.returnsFor(apps: _*)(users: _*)(developers: _*)
        DeveloperServiceMock.FetchUsers.returns(users: _*)
      }

    }

    "blankDevelopersPage" should {
      "show no results when initially opened" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val result = developersController.blankDevelopersPage()(aLoggedInRequest)

        contentAsString(result) should include("Developers")

      }
    }

    "removeEmailPreferencesPage" should {
      "show input box when opened" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)

        val result = developersController.removeEmailPreferencesPage()(aSuperUserLoggedInRequest)

        contentAsString(result) should include("Remove service from all developers email preferences")

      }

      "show errors on incorrect values submission" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        when(mockDeveloperService.removeEmailPreferencesByService(eqTo("mtd-vat-1"))(*)).thenReturn(Future.successful(EmailPreferencesDeleteSuccessResult))

        val result = developersController.removeEmailPreferencesAction()(aSuperUserLoggedInRequest.withFormUrlEncodedBody(("serviceName", "")))

        contentAsString(result) should include("Provide a service name")
      }

      "show success panel on correct submission" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        when(mockDeveloperService.removeEmailPreferencesByService(eqTo("mtd-vat-1"))(*)).thenReturn(Future.successful(EmailPreferencesDeleteSuccessResult))

        val result = developersController.removeEmailPreferencesAction()(aSuperUserLoggedInRequest.withFormUrlEncodedBody(("serviceName", "mtd-vat-1")))

        contentAsString(result) should include("mtd-vat-1 deleted from all developers")
      }

      "show failure panel on error" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        when(mockDeveloperService.removeEmailPreferencesByService(eqTo("mtd-vat-1"))(*)).thenReturn(Future.successful(EmailPreferencesDeleteFailureResult))

        val result = developersController.removeEmailPreferencesAction()(aSuperUserLoggedInRequest.withFormUrlEncodedBody(("serviceName", "mtd-vat-1")))

        contentAsString(result) should include("Sorry, we’re experiencing technical difficulties")
      }

      "be unauthorized if normal USER" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        val result = developersController.removeEmailPreferencesPage()(aLoggedInRequest)

        status(result) shouldBe FORBIDDEN
      }
    }

    "developersCsv" should {

      "exports as csv" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
        givenNoDataSuppliedDelegateServices()

        private val userId1     = UserId.random
        private val mfaDetails1 = List.empty
        private val emailPref1  = EmailPreferences(
          List(
            TaxRegimeInterests("VAT", Set("hello-world")),
            TaxRegimeInterests("MTD", Set("mtd-api", "other-api"))
          ),
          Set.empty
        )
        private val user1       =
          RegisteredUser(LaxEmailAddress("developer1@example.com"), userId1, "first", "last", verified = true, mfaDetails = mfaDetails1, emailPreferences = emailPref1)

        private val userId2     = UserId.random
        private val mfaDetails2 = List(
          SmsMfaDetail(MfaId(UUID.randomUUID()), "Dev2's phone", instant, "01234 567890", true),
          AuthenticatorAppMfaDetail(MfaId(UUID.randomUUID()), "Dev2's app", instant, false)
        )
        private val emailPref2  = EmailPreferences(List(TaxRegimeInterests("VAT", Set.empty)), Set.empty)
        private val user2       =
          RegisteredUser(LaxEmailAddress("developer2@example.com"), userId2, "first", "last", verified = true, mfaDetails = mfaDetails2, emailPreferences = emailPref2)

        private val userId3     = UserId.random
        private val mfaDetails3 = List(
          SmsMfaDetail(MfaId(UUID.randomUUID()), "Dev3's phone", instant, "01234 567890", false),
          AuthenticatorAppMfaDetail(MfaId(UUID.randomUUID()), "Dev3's app", instant, true)
        )
        private val emailPref3  = EmailPreferences(List.empty, Set(EVENT_INVITES, RELEASE_SCHEDULES, TECHNICAL, BUSINESS_AND_POLICY))
        private val user3       =
          RegisteredUser(LaxEmailAddress("developer3@example.com"), userId3, "first", "last", verified = true, mfaDetails = mfaDetails3, emailPreferences = emailPref3)

        private val xmlOrg1 = XmlOrganisation(
          OrganisationId(UUID.randomUUID()),
          VendorId(1),
          "xml org name 1",
          List(Collaborator(userId3, LaxEmailAddress("developer3@example.com")))
        )
        private val xmlOrg2 = XmlOrganisation(
          OrganisationId(UUID.randomUUID()),
          VendorId(2),
          "xml org name 2",
          List(Collaborator(userId1, LaxEmailAddress("developer1@example.com")), Collaborator(userId3, LaxEmailAddress("developer3@example.com")))
        )

        DeveloperServiceMock.FetchUsers.returns(user1, user2, user3)
        XmlServiceMock.GetAllXmlOrganisations.returns(List(xmlOrg1, xmlOrg2))

        val result = developersController.developersCsv()(aLoggedInRequest)
        contentAsString(result) should be(
          s"UserId,SMS MFA Active,Authenticator MFA Active,Business And Policy Email,Technical Email,Release Schedules Email,Event Invites Email,Full Category Emails,Individual APIs Emails,XML Vendors\n" +
            s"${userId1.toString},false,false,false,false,false,false,0,3,1\n" +
            s"${userId2.toString},true,false,false,false,false,false,1,0,0\n" +
            s"${userId3.toString},false,true,true,true,true,true,0,0,2\n"
        )
      }

      "fails without auth" in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        givenNoDataSuppliedDelegateServices()

        val result = developersController.developersCsv()(aLoggedInRequest)
        status(result) should be(FORBIDDEN)
      }
    }

    "developersPage" should {

      "show no results when initially opened" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val result = developersController.developersPage()(aLoggedInRequest.withFormUrlEncodedBody())

        contentAsString(result) should include("Developers")

      }

      "searching with all empty filters does not trigger a query" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val EMPTY = ""

        DeveloperServiceMock.SearchDevelopers.returns()

        val request = aLoggedInRequest.withFormUrlEncodedBody(
          "textFilter"            -> EMPTY,
          "apiVersionFilter"      -> EMPTY,
          "environmentFilter"     -> EMPTY,
          "developerStatusFilter" -> EMPTY
        )
        val result  = developersController.developersPage()(request)

        await(result)

        verify(mockDeveloperService, never).searchDevelopers(*)(*)
      }

      "allow searching by email or partial email" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress        = "developer@example.com"
        private val partialEmailAddress = "example"
        private val user                = aUser(emailAddress.toLaxEmail)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("textFilter" -> partialEmailAddress)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(Some(partialEmailAddress))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "remember the search filter text on submit" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val searchFilter = "anEmailFilterCriteria"

        DeveloperServiceMock.SearchDevelopers.returns()

        val request = aLoggedInRequest.withFormUrlEncodedBody("textFilter" -> searchFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(s"""value="$searchFilter"""")
      }

      "allow me to copy all the email addresses for verified users" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com".toLaxEmail
        private val email2 = "b@example.com".toLaxEmail
        private val email3 = "c@example.com".toLaxEmail

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1, true), aUser(email2, true), aUser(email3))

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> "ALL")
        val result                                                    = developersController.developersPage()(request)

        contentAsString(result) should include(s"${email1.text}; ${email2.text}")
      }

      "search by api version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress                   = "developer@example.com"
        private val user                           = aUser(emailAddress.toLaxEmail)
        private val apiDefinitionValueFromDropDown = "api-definition__1.0"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("apiVersionFilter" -> apiDefinitionValueFromDropDown)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val filter         = ApiContextVersion(ApiContext("api-definition"), apiVersion1)
        val expectedFilter = DevelopersSearchFilter(maybeApiFilter = Some(filter))
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "show an api version filter dropdown with correct display text" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiVersions   = Map(
          apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.ALPHA, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN),
          apiVersion2 -> ApiVersion(apiVersion2, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)
        )
        val apiDefinition = ApiDefinition(ServiceName(""), "", "MyApi", "", ApiContext.random, apiVersions, false, None, List(ApiCategory.OTHER))
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"MyApi (${apiVersion1.value}) (Alpha)")
        contentAsString(result) should include(s"MyApi (${apiVersion2.value}) (Stable)")

      }

      "show an api version filter dropdown with correct values for form submit with context and version" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        val apiContext = ApiContext.random

        val apiVersions = Map(
          apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN),
          apiVersion2 -> ApiVersion(apiVersion2, ApiStatus.STABLE, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN)
        )

        val apiDefinition = ApiDefinition(ServiceName(""), "", "", "", apiContext, apiVersions, false, None, List(ApiCategory.OTHER))
        FetchAllApiDefinitions.inAny.returns(apiDefinition)

        val result = developersController.developersPage()(aLoggedInRequest)

        contentAsString(result) should include(s"${apiContext.value}__${apiVersion1.value}")
        contentAsString(result) should include(s"${apiContext.value}__${apiVersion2.value}")

      }

      "show an api version filter dropdown without duplicates" in new Setup {
        val apiContext = ApiContext.random

        val apiVersions   = Map(apiVersion1 -> ApiVersion(apiVersion1, ApiStatus.ALPHA, ApiAccess.PUBLIC, List.empty, false, None, ApiVersionSource.UNKNOWN))
        val apiDefinition = ApiDefinition(ServiceName(""), "", "MyApi", "", apiContext, apiVersions, false, None, List(ApiCategory.OTHER))

        val result = developersController.getApiVersionsDropDownValues(List(apiDefinition, apiDefinition))

        result.size shouldBe 1
        result.head.value shouldBe s"${apiContext.value}__${apiVersion1.value}"
      }

      "show number of entries" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val email1 = "a@example.com"
        private val email2 = "b@example.com"

        DeveloperServiceMock.SearchDevelopers.returns(aUser(email1.toLaxEmail), aUser(email2.toLaxEmail))

        implicit val request: FakeRequest[AnyContentAsFormUrlEncoded] = aLoggedInRequest.withFormUrlEncodedBody("textFilter" -> "not relevant")
        val result                                                    = developersController.developersPage()(request)

        contentAsString(result) should include("Showing 2 entries")
      }

      "allow searching by developerStatusFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress = "developer@example.com"
        private val statusFilter = "VERIFIED"
        private val user         = aUser(emailAddress.toLaxEmail)

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("developerStatusFilter" -> statusFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }

      "allow searching by environmentFilter" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
        givenNoDataSuppliedDelegateServices()

        private val emailAddress      = "developer@example.com"
        private val user              = aUser(emailAddress.toLaxEmail)
        private val environmentFilter = "PRODUCTION"

        // Note: Developers is both users and collaborators
        DeveloperServiceMock.SearchDevelopers.returns(user)

        val request = aLoggedInRequest.withFormUrlEncodedBody("environmentFilter" -> environmentFilter)
        val result  = developersController.developersPage()(request)

        contentAsString(result) should include(emailAddress)

        val expectedFilter = DevelopersSearchFilter(environmentFilter = ProductionEnvironment)
        verify(mockDeveloperService).searchDevelopers(eqTo(expectedFilter))(*)
      }
    }
  }
}
