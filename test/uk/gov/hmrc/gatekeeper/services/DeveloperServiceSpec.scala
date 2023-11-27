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

package uk.gov.hmrc.gatekeeper.services

import java.time.{LocalDateTime, Period}
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

import mocks.connectors._
import mocks.services.XmlServiceMockProvider
import org.mockito.invocation.InvocationOnMock
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, ApplicationState}
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborator
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{UserId, _}
import uk.gov.hmrc.apiplatform.modules.common.utils.{AsyncHmrcSpec, FixedClock}
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.xml.{OrganisationId, VendorId, XmlOrganisation}
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker

class DeveloperServiceSpec extends AsyncHmrcSpec with CollaboratorTracker {

  def aUser(name: String, verified: Boolean = true, emailPreferences: EmailPreferences = EmailPreferences.noPreferences) = {
    val email = s"$name@example.com".toLaxEmail
    RegisteredUser(email, idOf(email), "Fred", "Example", verified, emailPreferences = emailPreferences)
  }

  def aDeveloper(name: String, apps: List[Application] = List.empty, verified: Boolean = true) = {
    val email = s"$name@example.com".toLaxEmail
    Developer(
      RegisteredUser(email, idOf(email), name, s"${name}son", verified),
      apps
    )
  }

  def anUnregisteredDeveloper(name: String, apps: List[Application] = List.empty) = {
    val email = s"$name@example.com".toLaxEmail
    Developer(
      UnregisteredUser(email, idOf(email)),
      apps
    )
  }

  def anApp(name: String, collaborators: Set[Collaborator], deployedTo: Environment = Environment.PRODUCTION): ApplicationResponse = {
    val grantLength: Period = Period.ofDays(547)
    ApplicationResponse(
      ApplicationId.random,
      ClientId("clientId"),
      "gatewayId",
      name,
      deployedTo,
      None,
      collaborators,
      LocalDateTime.now(),
      Some(LocalDateTime.now()),
      Standard(),
      ApplicationState(),
      grantLength
    )
  }

  def aProdApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = Environment.PRODUCTION)

  def aSandboxApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = Environment.SANDBOX)

  val prodAppId = ApplicationId.random

  trait Setup extends MockitoSugar
      with ArgumentMatchersSugar
      with ApplicationConnectorMockProvider
      with CommandConnectorMockProvider
      with DeveloperConnectorMockProvider
      with XmlServiceMockProvider {
    val mockAppConfig = mock[AppConfig]

    val underTest = new DeveloperService(
      mockAppConfig,
      mockDeveloperConnector,
      mockSandboxApplicationConnector,
      mockProductionApplicationConnector,
      CommandConnectorMock.aMock,
      mockXmlService,
      FixedClock.clock
    )

    val verifiedAdminUser           = aUser("admin1")
    val verifiedAdminCollaborator   = verifiedAdminUser.email.asAdministratorCollaborator
    val unverifiedUser              = aUser("admin2", verified = false)
    val unverifiedAdminCollaborator = unverifiedUser.email.asAdministratorCollaborator
    val developerUser               = aUser("developer1")
    val developerCollaborator       = developerUser.email.asDeveloperCollaborator
    val commonUsers                 = List(verifiedAdminUser, unverifiedUser, developerUser)
    val apiContext                  = ApiContext("api")
    val apiVersion                  = ApiVersionNbr.random

    val orgOne = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()))

    val xmlServiceNames = Set("XML API one", "XML API two")
    val offset          = 0
    val limit           = 4

    implicit val hc = HeaderCarrier()

    def fetchDeveloperWillReturn(
        user: RegisteredUser,
        includeDeleted: FetchDeletedApplications,
        productionApps: List[ApplicationResponse] = List.empty,
        sandboxApps: List[ApplicationResponse] = List.empty
      ) = {
      DeveloperConnectorMock.FetchByEmail.handles(user)
      DeveloperConnectorMock.FetchByUserId.handles(user)
      DeveloperConnectorMock.FetchById.handles(user)
      XmlServiceMock.GetXmlServicesForUser.returnsApis(user, xmlServiceNames)
      XmlServiceMock.GetXmlOrganisationsForUser.returnsOrganisations(user.userId, List(orgOne))

      includeDeleted match {
        case FetchDeletedApplications.Include => {
          ApplicationConnectorMock.Prod.FetchApplicationsByUserId.returns(productionApps: _*)
          ApplicationConnectorMock.Sandbox.FetchApplicationsByUserId.returns(sandboxApps: _*)
        }
        case FetchDeletedApplications.Exclude => {
          ApplicationConnectorMock.Prod.FetchApplicationsExcludingDeletedByUserId.returns(productionApps: _*)
          ApplicationConnectorMock.Sandbox.FetchApplicationsExcludingDeletedByUserId.returns(sandboxApps: _*)
        }
      }
    }

    def fetchDevelopersWillReturnTheRequestedUsers = {
      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenAnswer((invocationOnMock: InvocationOnMock) => {
        val developersRequested = invocationOnMock.getArguments()(0).asInstanceOf[Iterable[LaxEmailAddress]].toSet
        successful(commonUsers.filter(user => developersRequested.contains(user.email)))
      })
    }

    // TODO
    def deleteDeveloperWillSucceed = {
      when(mockDeveloperConnector.deleteDeveloper(*)(*))
        .thenReturn(successful(DeveloperDeleteSuccessResult))
      CommandConnectorMock.IssueCommand.ToRemoveCollaborator.succeeds()
    }

    // def verifyTheActor(actor: Actor)(cmd: ApplicationCommand)                           = cmd.actor shouldBe actor
    def verifyIsGatekeeperUser(gatekeeperUserName: String)(cmd: ApplicationCommands.RemoveCollaborator)     = cmd.actor shouldBe Actors.GatekeeperUser(gatekeeperUserName)
    def verifyIsAppCollaborator(emailAddress: LaxEmailAddress)(cmd: ApplicationCommands.RemoveCollaborator) = cmd.actor shouldBe Actors.AppCollaborator(emailAddress)

    def verifyCollaboratorRemovedEmailIs(email: LaxEmailAddress)(cmd: ApplicationCommands.RemoveCollaborator) = cmd.collaborator.emailAddress == email

    def verifyCollaboratorRemovedFromApp(
        app: Application,
        userToRemove: LaxEmailAddress,
        gatekeeperUserName: String,
        adminsToEmail: Set[LaxEmailAddress]
      ) = {
      inside(CommandConnectorMock.IssueCommand.verifyCommand(app.id)) {
        case cmd @ ApplicationCommands.RemoveCollaborator(foundActor, foundCollaborator, foundAdminsToEmail) =>
          verifyIsGatekeeperUser(gatekeeperUserName)(cmd)
          verifyCollaboratorRemovedEmailIs(userToRemove)(cmd)
        case _                                                                                               => fail("Wrong command")
      }
    }

    def removeMfaReturnWillReturn(user: RegisteredUser) = {
      when(mockDeveloperConnector.removeMfa(*, *)(*)).thenReturn(successful(user))
    }
  }

  "developerService" should {

    "filter all users (no unregistered collaborators)" in new Setup {
      val applications = List(
        anApp(
          "application1",
          Set(
            "Bob@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jacob@example.com".toLaxEmail.asDeveloperCollaborator
          )
        )
      )

      val bob   = aDeveloper("Bob", applications)
      val jim   = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob, jim, jacob)

      val result = underTest.filterUsersBy(AllUsers, applications)(users)

      result should contain allElementsOf List(bob, jim, jacob)
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val applications = List(
        anApp(
          "application1",
          Set(
            "Bob@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jacob@example.com".toLaxEmail.asDeveloperCollaborator
          )
        ),
        anApp(
          "application2",
          Set(
            "Julia@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jim@example.com".toLaxEmail.asDeveloperCollaborator
          )
        )
      )

      val bob   = aDeveloper("Bob", applications)
      val jim   = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob, jim, jacob)

      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result should contain allElementsOf List(bob, jim, jacob, anUnregisteredDeveloper("Julia", applications.tail))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val applications = List(
        anApp(
          "application1",
          Set(
            "Bob@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jacob@example.com".toLaxEmail.asDeveloperCollaborator
          )
        ),
        anApp(
          "application2",
          Set(
            "Julia@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jim@example.com".toLaxEmail.asDeveloperCollaborator
          )
        )
      )

      val bob   = aDeveloper("Bob", applications)
      val jim   = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob, jim, jacob)

      val result = underTest.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result should contain allElementsOf List(bob, jim, jacob, anUnregisteredDeveloper("Julia", applications.tail))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val applications = List(
          anApp(
            "application1",
            Set(
              "Shirley@example.com".toLaxEmail.asAdministratorCollaborator,
              "Jacob@example.com".toLaxEmail.asDeveloperCollaborator
            )
          ),
          anApp(
            "application2",
            Set(
              "Julia@example.com".toLaxEmail.asAdministratorCollaborator,
              "Jim@example.com".toLaxEmail.asDeveloperCollaborator
            )
          )
        )

        val gaia    = aDeveloper("Gaia")
        val jimbob  = aDeveloper("Jimbob")
        val shirley = aDeveloper("Shirley")
        val users   = List(shirley, jimbob, gaia)

        val result = underTest.filterUsersBy(NoApplications, applications)(users)
        result should contain allElementsOf List(gaia, jimbob)
      }

    "filter users who have no subscriptions" in new Setup {
      val _allApplications = List(
        anApp(
          "application1",
          Set(
            "Bob@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jim@example.com".toLaxEmail.asDeveloperCollaborator,
            "Jacob@example.com".toLaxEmail.asDeveloperCollaborator
          )
        ),
        anApp(
          "application2",
          Set(
            "Julia@example.com".toLaxEmail.asAdministratorCollaborator,
            "Jim@example.com".toLaxEmail.asDeveloperCollaborator
          )
        )
      )

      val gaia    = aDeveloper("Gaia")
      val jimbob  = aDeveloper("Jimbob")
      val shirley = aDeveloper("Shirley")
      val jim     = aDeveloper("Jim", _allApplications)
      val users   = List(shirley, jimbob, gaia, jim)

      val result = underTest.filterUsersBy(NoSubscriptions, _allApplications)(users)

      result should have size 4

      result should contain allElementsOf List(
        jim,
        anUnregisteredDeveloper("Bob", List(_allApplications.head)),
        anUnregisteredDeveloper("Jacob", List(_allApplications.head)),
        anUnregisteredDeveloper("Julia", List(_allApplications.tail.head))
      )
    }

    "filter by status does no filtering when any status" in new Setup {
      val users  = List(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val bob    = aDeveloper("Bob", verified = false)
      val brian  = aDeveloper("Brian")
      val sheila = aDeveloper("Sheila")
      val users  = List(bob, brian, sheila)

      val result = underTest.filterUsersBy(VerifiedStatus)(users)

      result shouldBe List(brian, sheila)
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val bob    = aDeveloper("Bob", verified = false)
      val brian  = aDeveloper("Brian")
      val sheila = aDeveloper("Sheila")
      val users  = List(bob, brian, sheila)

      val result = underTest.filterUsersBy(UnverifiedStatus)(users)

      result shouldBe List(bob)
    }

    "fetch the developer and the applications they are a team member on" in new Setup {
      val developer = aUser("Fred")
      val apps      = List(anApp("application", Set(developer.email.asAdministratorCollaborator)))
      fetchDeveloperWillReturn(developer, FetchDeletedApplications.Include, apps)

      val result = await(underTest.fetchDeveloper(developer.userId, FetchDeletedApplications.Include))

      result shouldBe Developer(developer, apps, xmlServiceNames, List(orgOne))
      verify(mockDeveloperConnector).fetchById(eqTo(UuidIdentifier(developer.userId)))(*)
      verify(mockProductionApplicationConnector).fetchApplicationsByUserId(eqTo(developer.userId))(*)
    }

    "remove MFA" in new Setup {
      val developer            = aUser("Fred")
      val developerId          = UuidIdentifier(developer.userId)
      val loggedInUser: String = "admin-user"
      removeMfaReturnWillReturn(developer)

      val result = await(underTest.removeMfa(developerId, loggedInUser))

      result shouldBe developer
      verify(mockDeveloperConnector).removeMfa(developerId, loggedInUser)
    }
  }

  "fetchDeveloper" should {
    val emailPreferences = EmailPreferences(
      interests = List(
        TaxRegimeInterests("TestRegimeOne", Set("TestServiceOne", "TestServiceTwo")),
        TaxRegimeInterests("TestRegimeTwo", Set("TestServiceThree", "TestServiceFour"))
      ),
      topics = Set(EmailTopic.TECHNICAL, EmailTopic.BUSINESS_AND_POLICY)
    )
    val user             = aUser("Fred", emailPreferences = emailPreferences)

    "fetch the developer when requested by userId" in new Setup {
      fetchDeveloperWillReturn(user, FetchDeletedApplications.Include)

      await(underTest.fetchDeveloper(user.userId, FetchDeletedApplications.Include)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
    }

    "fetch the developer not including deleted applications when requested by userId" in new Setup {
      fetchDeveloperWillReturn(user, FetchDeletedApplications.Exclude)

      await(underTest.fetchDeveloper(user.userId, FetchDeletedApplications.Exclude)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
    }

    "fetch the developer when requested by email as developerId" in new Setup {
      fetchDeveloperWillReturn(user, FetchDeletedApplications.Include)

      await(underTest.fetchDeveloper(EmailIdentifier(user.email), FetchDeletedApplications.Include)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
    }

    "fetch the developer when requested by userId as developerId" in new Setup {
      fetchDeveloperWillReturn(user, FetchDeletedApplications.Include)

      await(underTest.fetchDeveloper(UuidIdentifier(user.userId), FetchDeletedApplications.Include)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
    }

    "returns UpstreamErrorResponse when call to GetXmlServicesForUser fails" in new Setup {
      DeveloperConnectorMock.FetchByEmail.handles(user)
      DeveloperConnectorMock.FetchByUserId.handles(user)
      DeveloperConnectorMock.FetchById.handles(user)
      XmlServiceMock.GetXmlServicesForUser.returnsError(user)

      intercept[UpstreamErrorResponse](
        await(underTest.fetchDeveloper(UuidIdentifier(user.userId), FetchDeletedApplications.Include)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
      ) match {
        case (e: UpstreamErrorResponse) => succeed
        case _                          => fail()
      }
    }

    "returns UpstreamErrorResponse when call to get xml organisations fails" in new Setup {
      DeveloperConnectorMock.FetchByEmail.handles(user)
      DeveloperConnectorMock.FetchByUserId.handles(user)
      DeveloperConnectorMock.FetchById.handles(user)
      XmlServiceMock.GetXmlServicesForUser.returnsApis(user, Set.empty)
      XmlServiceMock.GetXmlOrganisationsForUser.returnsError()

      intercept[UpstreamErrorResponse](
        await(underTest.fetchDeveloper(UuidIdentifier(user.userId), FetchDeletedApplications.Include)) shouldBe Developer(user, List.empty, xmlServiceNames, List(orgOne))
      ) match {
        case (e: UpstreamErrorResponse) => succeed
        case _                          => fail()
      }
    }
  }

  "developerService.deleteDeveloper" should {
    val gatekeeperUserId = "gate.keeper"
    val user             = aUser("Fred")
    val developerId      = UuidIdentifier(user.userId)

    "delete the developer if they have no associated apps in either sandbox or production" in new Setup {

      fetchDeveloperWillReturn(user, FetchDeletedApplications.Exclude, productionApps = List.empty, sandboxApps = List.empty)
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email.text)))(*)
      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }

    "remove the user from their apps and email other verified admins on each production app before deleting the user" in new Setup {
      val app1 = aProdApp("application1", Set(verifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app2 = aProdApp("application2", Set(unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app3 = aProdApp("application3", Set(verifiedAdminCollaborator, unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))

      fetchDeveloperWillReturn(user, FetchDeletedApplications.Exclude, List(app1, app2, app3))
      fetchDevelopersWillReturnTheRequestedUsers
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyCollaboratorRemovedFromApp(app1, user.email, gatekeeperUserId, Set(verifiedAdminCollaborator.emailAddress))
      verifyCollaboratorRemovedFromApp(app2, user.email, gatekeeperUserId, Set.empty)
      verifyCollaboratorRemovedFromApp(app3, user.email, gatekeeperUserId, Set(verifiedAdminCollaborator.emailAddress))

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email.text)))(*)
    }

    "remove the user from their apps without emailing other verified admins on each sandbox app before deleting the user" in new Setup {
      val app1 = aSandboxApp("application1", Set(verifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app2 = aSandboxApp("application2", Set(unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app3 = aSandboxApp("application3", Set(verifiedAdminCollaborator, unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))

      fetchDeveloperWillReturn(user, FetchDeletedApplications.Exclude, List.empty, List(app1, app2, app3))
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyCollaboratorRemovedFromApp(app1, user.email, gatekeeperUserId, Set.empty)
      verifyCollaboratorRemovedFromApp(app2, user.email, gatekeeperUserId, Set.empty)
      verifyCollaboratorRemovedFromApp(app3, user.email, gatekeeperUserId, Set.empty)

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email.text)))(*)
    }

    "fail if the developer is the sole admin on any of their associated apps" in new Setup {

      val productionApps = List(anApp("productionApplication", Set(user.email.asAdministratorCollaborator)))
      val sandboxApps    = List(anApp(
        name = "sandboxApplication",
        collaborators = Set(user.email.asDeveloperCollaborator, "another@example.com".toLaxEmail.asAdministratorCollaborator)
      ))
      fetchDeveloperWillReturn(user, FetchDeletedApplications.Exclude, productionApps, sandboxApps)

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(*)(*)
      CommandConnectorMock.IssueCommand.verifyNoCommandsIssued()
    }
  }

  "developerService searchDevelopers" should {
    "find users" in new Setup {
      private val user        = aUser("fred")
      private val emailFilter = "example"
      DeveloperConnectorMock.SearchDevelopers.returns(user)

      val filter = DevelopersSearchFilter(maybeEmailFilter = Some(emailFilter))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user)

      verify(mockDeveloperConnector).searchDevelopers(Some(emailFilter), DeveloperStatusFilter.AllStatus)
    }

    "find by api context and version" in new Setup {
      val user1 = aUser("production")
      val user2 = aUser("sandbox")

      private val email1 = user1.email
      private val email2 = user2.email

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(email1)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(email2)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1, email2))(user1, user2)

      val filter = DevelopersSearchFilter(environmentFilter = AnyEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)

      verify(mockProductionApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
      verify(mockSandboxApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
    }

    "find by api context and version where same email in production and sandbox" in new Setup {
      val user = aUser("user")

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(user.email)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(user.email)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(user.email))(user)

      val filter = DevelopersSearchFilter(maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user)
    }

    "find by api context and version with email filter with a collaborator who is not registered" in new Setup {

      val user1 = aUser("user1")
      val user2 = aUser("user2")
      val user3 = aUser("user3")

      private val email1 = user1.email
      private val email2 = user2.email
      private val email3 = user3.email

      val emailFilter = "emailFilter"

      ApplicationConnectorMock.Prod.SearchCollaborators.returnsFor(apiContext, apiVersion, Some(emailFilter))(email1, email2, email3)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returnsFor(apiContext, apiVersion, Some(emailFilter))()
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1, email2, email3))(user1, user2)

      val filter = DevelopersSearchFilter(maybeEmailFilter = Some(emailFilter), maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)
    }

    "find by developer status" in new Setup {
      val user1 = aUser("user1")
      DeveloperConnectorMock.SearchDevelopers.returnsFor(None, DeveloperStatusFilter.VerifiedStatus)(user1)

      val filter = DevelopersSearchFilter(None, None, developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)
      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1)

      verify(mockDeveloperConnector).searchDevelopers(None, DeveloperStatusFilter.VerifiedStatus)
    }

    "find by api context, version and developer status" in new Setup {

      val user1 = aUser("user1", verified = true)
      val user2 = aUser("user2", verified = true)
      val user3 = aUser("user3", verified = false)
      val user4 = aUser("user4", verified = true)

      private val email1 = user1.email
      private val email2 = user2.email
      private val email3 = user3.email
      private val email4 = user4.email

      val emailFilter = "emailFilter"

      ApplicationConnectorMock.Prod.SearchCollaborators.returnsFor(apiContext, apiVersion, Some(emailFilter))(email1, email2, email3, email4)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returnsFor(apiContext, apiVersion, Some(emailFilter))()

      DeveloperConnectorMock.FetchByEmails.returns(user1, user2, user3)

      val filter = DevelopersSearchFilter(
        maybeEmailFilter =
          Some(emailFilter),
        maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)),
        developerStatusFilter = DeveloperStatusFilter.VerifiedStatus
      )

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)

      verify(mockDeveloperConnector).fetchByEmails(Set(email1, email2, email3, email4))
    }

    "find by developer status should sort users by email" in new Setup {

      val firstInTheListUser  = RegisteredUser("101@example.com".toLaxEmail, UserId.random, "alphaFirstName", "alphaLastName", true)
      val secondInTheListUser = RegisteredUser("lalala@example.com".toLaxEmail, UserId.random, "betaFirstName", "betaLastName", false)
      val thirdInTheListUser  = RegisteredUser("zigzag@example.com".toLaxEmail, UserId.random, "thetaFirstName", "thetaLastName", false)
      DeveloperConnectorMock.SearchDevelopers.returnsFor(None, DeveloperStatusFilter.AllStatus)(thirdInTheListUser, firstInTheListUser, secondInTheListUser)

      val filter = DevelopersSearchFilter(None, None, developerStatusFilter = DeveloperStatusFilter.AllStatus)
      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(firstInTheListUser, secondInTheListUser, thirdInTheListUser)

      verify(mockDeveloperConnector).searchDevelopers(None, DeveloperStatusFilter.AllStatus)
    }

    "find by api context and version and Production environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser    = aUser("sandbox")

      private val email1 = productionUser.email

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(productionUser.email)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(sandboxUser.email)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1))(productionUser)

      val filter = DevelopersSearchFilter(environmentFilter = ProductionEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(productionUser)

      verify(mockProductionApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
      verify(mockSandboxApplicationConnector, never).searchCollaborators(*[ApiContext], *[ApiVersionNbr], *)(*)
    }

    "find by api context and version and Sandbox environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser    = aUser("sandbox")

      private val email2 = sandboxUser.email

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(productionUser.email)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(sandboxUser.email)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email2))(sandboxUser)

      val filter = DevelopersSearchFilter(environmentFilter = SandboxEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(sandboxUser)

      verify(mockProductionApplicationConnector, never).searchCollaborators(*[ApiContext], *[ApiVersionNbr], *)(*)
      verify(mockSandboxApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
    }
  }

  "developerService fetchDevelopersByEmailPreferences" should {
    val topic       = TopicOptionChoice.BUSINESS_AND_POLICY
    val sandboxUser = aUser("sandbox")
    val category1   = ApiCategory.AGENTS
    val category2   = ApiCategory.BUSINESS_RATES
    val categories  = Set[ApiCategory](category1, category2)
    val apiName1    = "apiName1"
    val apiName2    = "apiName2"
    val apiName3    = "apiName3"
    val apis        = List(apiName1, apiName2, apiName3)

    "call the connector correctly when passed a topic and a category" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, None, Some(Set(category1)), false)(sandboxUser)

      val result = await(underTest.fetchDevelopersByAPICategoryEmailPreferences(topic, category1))

      result shouldBe List(sandboxUser)

      verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), *, eqTo(Some(Set(category1))), *)(*)
    }

    "call the connector correctly passed a topic, a sequence of categories and apis" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, Some(apis), Some(categories), false)(sandboxUser)

      val result = await(underTest.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apis, false))

      result shouldBe List(sandboxUser)

      verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), eqTo(Some(apis)), eqTo(Some(categories)), eqTo(false))(*)
    }

    "call the connector correctly passed a topic, a sequence of categories and apis and private match is set" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, Some(apis), Some(categories), true)(sandboxUser)

      val result = await(underTest.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apis, true))

      result shouldBe List(sandboxUser)

      verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), eqTo(Some(apis)), eqTo(Some(categories)), eqTo(true))(*)
    }
  }

  "developerService fetchDevelopersByEmailPreferencesPaginated" should {
    val topic       = TopicOptionChoice.BUSINESS_AND_POLICY
    val sandboxUser = aUser("sandbox")
    val category1   = ApiCategory.VAT
    val category2   = ApiCategory.BUSINESS_RATES
    val categories  = Set[ApiCategory](category1, category2)
    val apiName1    = "apiName1"
    val apiName2    = "apiName2"
    val apiName3    = "apiName3"
    val apis        = List(apiName1, apiName2, apiName3)

    "call the connector correctly when only passed a category" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferencesPaginated.returnsFor(None, None, Some(Set(category1)), privateApiMatch = false, offset, limit)(sandboxUser)

      val result = await(underTest.fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(Set(category1), offset, limit))

      result shouldBe UserPaginatedResponse(1, List(sandboxUser))

      verify(mockDeveloperConnector).fetchByEmailPreferencesPaginated(*, *, eqTo(Some(Set(category1))), eqTo(false), eqTo(offset), eqTo(limit))(*)
    }

    "call the connector correctly when only passed a topic" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferencesPaginated.returnsFor(Some(topic), None, None, privateApiMatch = false, offset, limit)(sandboxUser)

      val result = await(underTest.fetchDevelopersByEmailPreferencesPaginated(Some(topic), None, None, privateApiMatch = false, offset, limit))

      result shouldBe UserPaginatedResponse(1, List(sandboxUser))

      verify(mockDeveloperConnector).fetchByEmailPreferencesPaginated(eqTo(Some(topic)), *, *, eqTo(false), eqTo(offset), eqTo(limit))(*)
    }

    "call the connector correctly when only passed a service" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferencesPaginated.returnsFor(Some(topic), Some(apis), Some(categories), privateApiMatch = false, offset, limit)(sandboxUser)

      val result = await(underTest.fetchDevelopersByEmailPreferencesPaginated(Some(topic), Some(apis), Some(categories), privateApiMatch = false, offset, limit))

      result shouldBe UserPaginatedResponse(1, List(sandboxUser))

      verify(mockDeveloperConnector).fetchByEmailPreferencesPaginated(eqTo(Some(topic)), eqTo(Some(apis)), eqTo(Some(categories)), eqTo(false), eqTo(offset), eqTo(limit))(*)
    }
  }

  "developerService fetchDevelopersBySpecificTaxRegimesEmailPreferences" should {
    val sandboxUser = aUser("sandbox")
    val category1   = ApiCategory.VAT
    "call the connector correctly when only passed a tax regime" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferencesPaginated.returnsFor(None, None, Some(Set(category1)), privateApiMatch = false, offset, limit)(sandboxUser)
      val result = await(underTest.fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(Set(category1), offset, limit))

      result shouldBe UserPaginatedResponse(1, List(sandboxUser))

      verify(mockDeveloperConnector).fetchByEmailPreferencesPaginated(*, *, eqTo(Some(Set(category1))), eqTo(false), eqTo(offset), eqTo(limit))(*)
    }
  }

  "developerService fetchDevelopersBySpecificApisEmailPreferences" should {
    val sandboxUser = aUser("sandbox")
    val apiName1    = "apiName1"

    "call the connector correctly when only passed apis" in new Setup {
      DeveloperConnectorMock.FetchByEmailPreferencesPaginated.returnsFor(None, Some(Seq(apiName1)), None, privateApiMatch = false, offset, limit)(sandboxUser)

      val result = await(underTest.fetchDevelopersBySpecificApisEmailPreferences(List(apiName1), offset, limit))

      result shouldBe UserPaginatedResponse(1, List(sandboxUser))

      verify(mockDeveloperConnector).fetchByEmailPreferencesPaginated(*, eqTo(Some(Seq(apiName1))), *, eqTo(false), eqTo(offset), eqTo(limit))(*)
    }
  }
}
