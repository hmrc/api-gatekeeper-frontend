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

package services

import java.util.UUID

import config.AppConfig
import connectors._
import model.Developer.createUnregisteredDeveloper
import model.{DeveloperStatusFilter, _}
import org.joda.time.DateTime
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.mockito.invocation.InvocationOnMock
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class DeveloperServiceSpec extends UnitSpec with MockitoSugar with ArgumentMatchersSugar {

  def aUser(name: String, verified: Boolean = true) = User(s"$name@example.com", "Fred", "Example", Some(verified))

  def aDeveloper(name: String, apps: Seq[Application] = Seq.empty, verified: Boolean = true) =
    Developer(s"$name@example.com", name, s"${name}son", Some(verified), apps)

  def anApp(name: String, collaborators: Set[Collaborator], deployedTo: String = "PRODUCTION"): ApplicationResponse = {
    ApplicationResponse(ApplicationId(UUID.randomUUID().toString()), "clientId", "gatewayId", name, deployedTo, None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
  }

  def aProdApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = "PRODUCTION")

  def aSandboxApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = "SANDBOX")

  trait Setup {
    val mockProductionApplicationConnector = mock[ProductionApplicationConnector]
    val mockSandboxApplicationConnector = mock[SandboxApplicationConnector]
    val mockDeveloperConnector = mock[DeveloperConnector]
    val mockAppConfig = mock[AppConfig]

    val underTest = new DeveloperService(mockAppConfig, mockDeveloperConnector, mockSandboxApplicationConnector, mockProductionApplicationConnector)

    val verifiedAdminUser = aUser("admin1")
    val verifiedAdminTeamMember = Collaborator(verifiedAdminUser.email, CollaboratorRole.ADMINISTRATOR)
    val unverifiedAdminUser = aUser("admin2", verified = false)
    val unverifiedAdminTeamMember = Collaborator(unverifiedAdminUser.email, CollaboratorRole.ADMINISTRATOR)
    val developerUser = aUser("developer1")
    val developerTeamMember = Collaborator(developerUser.email, CollaboratorRole.DEVELOPER)
    val commonUsers = Seq(verifiedAdminUser, unverifiedAdminUser, developerUser)

    implicit val hc = HeaderCarrier()

    def fetchDeveloperWillReturn(developer: User, productionApps: Seq[ApplicationResponse], sandboxApps: Seq[ApplicationResponse] = Seq.empty) = {
      when(mockDeveloperConnector.fetchByEmail(*)(*))
        .thenReturn(Future.successful(developer))
      when(mockProductionApplicationConnector.fetchApplicationsByEmail(*)(*))
        .thenReturn(Future.successful(productionApps))
      when(mockSandboxApplicationConnector.fetchApplicationsByEmail(*)(*))
        .thenReturn(Future.successful(sandboxApps))
    }

    def fetchDevelopersWillReturnTheRequestedUsers = {
      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenAnswer((invocationOnMock: InvocationOnMock) => {
          val developersRequested = invocationOnMock.getArguments()(0).asInstanceOf[Iterable[String]].toSet
          Future.successful(commonUsers.filter(user => developersRequested.contains(user.email)))
      })
    }

    def deleteDeveloperWillSucceed = {
      when(mockDeveloperConnector.deleteDeveloper(*[DeleteDeveloperRequest])(*))
        .thenReturn(Future.successful(DeveloperDeleteSuccessResult))
      when(mockProductionApplicationConnector.removeCollaborator(*[ApplicationId], *, *, *)(*))
        .thenReturn(Future.successful(ApplicationUpdateSuccessResult))
      when(mockSandboxApplicationConnector.removeCollaborator(*[ApplicationId], *, *, *)(*))
        .thenReturn(Future.successful(ApplicationUpdateSuccessResult))
    }

    def verifyTeamMemberRemovedFromApp(app: Application,
                                       userToRemove: String,
                                       gatekeeperUserId: String,
                                       adminsToEmail: Seq[String],
                                       environment: String = "PRODUCTION") = {
      environment match {
        case "PRODUCTION" =>
          verify(mockProductionApplicationConnector).removeCollaborator(eqTo(app.id), eqTo(userToRemove),
            eqTo(gatekeeperUserId), eqTo(adminsToEmail))(*)
        case "SANDBOX" =>
          verify(mockSandboxApplicationConnector).removeCollaborator(eqTo(app.id), eqTo(userToRemove),
            eqTo(gatekeeperUserId), eqTo(adminsToEmail))(*)
      }
    }

    def removeMfaReturnWillReturn(user: User) = {
      when(mockDeveloperConnector.removeMfa(*, *)(*)).thenReturn(Future.successful(user))
    }
  }

  def bob(apps: Seq[Application] = Seq.empty) = aDeveloper("Bob", apps)

  def jim(apps: Seq[Application] = Seq.empty) = aDeveloper("Jim", apps)

  def jacob(apps: Seq[Application] = Seq.empty) = aDeveloper("Jacob", apps)

  def julia(apps: Set[Application]) = createUnregisteredDeveloper("Julia@example.com", apps)

  "developerService" should {

    "filter all users (no unregistered collaborators)" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.com", CollaboratorRole.DEVELOPER))))
      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications))
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.com", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.com", CollaboratorRole.DEVELOPER))))

      val users = Seq(bob(applications), jim(applications), jacob(applications))


      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result shouldBe Seq(bob(applications), jim(applications), jacob(applications), julia(applications.tail.toSet))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val applications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.com", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.com", CollaboratorRole.DEVELOPER))))

      val users = Seq(aDeveloper("Bob", applications), aDeveloper("Jim", applications), aDeveloper("Jacob", applications))


      val result = underTest.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result shouldBe Seq(aDeveloper("Bob", applications), aDeveloper("Jim", applications), aDeveloper("Jacob", applications), julia(applications.tail.toSet))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val applications = Seq(
          anApp("application1", Set(
            Collaborator("Shirley@example.com", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jacob@example.com", CollaboratorRole.DEVELOPER))),
          anApp("application2", Set(
            Collaborator("Julia@example.com", CollaboratorRole.ADMINISTRATOR),
            Collaborator("Jim@example.com", CollaboratorRole.DEVELOPER))))
        val users = Seq(aDeveloper("Shirley", applications), aDeveloper("Gaia"), aDeveloper("Jimbob"))


        val result = underTest.filterUsersBy(NoApplications, applications)(users)
        result shouldBe Seq(aDeveloper("Gaia"), aDeveloper("Jimbob"))
      }

    "filter users who have no subscriptions" in new Setup {
      val _allApplications = Seq(
        anApp("application1", Set(
          Collaborator("Bob@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jacob@example.com", CollaboratorRole.DEVELOPER))),
        anApp("application2", Set(
          Collaborator("Julia@example.com", CollaboratorRole.ADMINISTRATOR),
          Collaborator("Jim@example.com", CollaboratorRole.DEVELOPER))))
      val users = Seq(aDeveloper("Shirley"), aDeveloper("Gaia"), aDeveloper("Jimbob"), aDeveloper("Jim", _allApplications))

      val result = underTest.filterUsersBy(NoSubscriptions, _allApplications)(users)


      result should have size 4

      result shouldBe Seq(aDeveloper("Jim", _allApplications),
        createUnregisteredDeveloper("Bob@example.com", Set(_allApplications.head)),
        createUnregisteredDeveloper("Jacob@example.com", Set(_allApplications.head)),
        createUnregisteredDeveloper("Julia@example.com", Set(_allApplications.tail.head)))
    }

    "filter by status does no filtering when any status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(VerifiedStatus)(users)
      result shouldBe Seq(aDeveloper("Brian"), aDeveloper("Sheila"))
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val users = Seq(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(UnverifiedStatus)(users)
      result shouldBe Seq(aDeveloper("Bob", verified = false))
    }

    "fetch the developer and the applications they are a team member on" in new Setup {
      val developer = aUser("Fred")
      val apps = Seq(anApp("application", Set(Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))))
      fetchDeveloperWillReturn(developer, apps)

      val result = await(underTest.fetchDeveloper(developer.email))
      result.toDeveloper shouldBe developer.toDeveloper(apps)
      verify(mockDeveloperConnector).fetchByEmail(eqTo(developer.email))(*)
      verify(mockProductionApplicationConnector).fetchApplicationsByEmail(eqTo(developer.email))(*)
    }

    "remove MFA" in new Setup {
      val developer: User = aUser("Fred")
      val loggedInUser: String = "admin-user"
      removeMfaReturnWillReturn(developer)

      val result: User = await(underTest.removeMfa(developer.email, loggedInUser))

      result shouldBe developer
      verify(mockDeveloperConnector).removeMfa(developer.email, loggedInUser)
    }
  }

  "developerService.deleteDeveloper" should {

    "delete the developer if they have no associated apps in either sandbox or production" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
  
      fetchDeveloperWillReturn(developer, productionApps = Seq.empty, sandboxApps = Seq.empty)
      deleteDeveloperWillSucceed

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, developer.email)))(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(*[ApplicationId], *, *, *)(*)
      verify(mockSandboxApplicationConnector, never).removeCollaborator(*[ApplicationId], *, *, *)(*)
    }

    "remove the user from their apps and email other verified admins on each production app before deleting the user" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val user = aUser("Fred")
      val app1 = aProdApp("application1", Set(verifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))
      val app2 = aProdApp("application2", Set(unverifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))
      val app3 = aProdApp("application3", Set(verifiedAdminTeamMember, unverifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))

      fetchDeveloperWillReturn(user, Seq(app1, app2, app3))
      fetchDevelopersWillReturnTheRequestedUsers
      deleteDeveloperWillSucceed

      val result = await(underTest.deleteDeveloper(user.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyTeamMemberRemovedFromApp(app1, user.email, gatekeeperUserId, Seq(verifiedAdminTeamMember.emailAddress))
      verifyTeamMemberRemovedFromApp(app2, user.email, gatekeeperUserId, Seq.empty)
      verifyTeamMemberRemovedFromApp(app3, user.email, gatekeeperUserId, Seq(verifiedAdminTeamMember.emailAddress))

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email)))(*)
    }

    "remove the user from their apps without emailing other verified admins on each sandbox app before deleting the user" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val user = aUser("Fred")
      val app1 = aSandboxApp("application1", Set(verifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))
      val app2 = aSandboxApp("application2", Set(unverifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))
      val app3 = aSandboxApp("application3", Set(verifiedAdminTeamMember, unverifiedAdminTeamMember, Collaborator(user.email, CollaboratorRole.ADMINISTRATOR)))

      fetchDeveloperWillReturn(user, Seq.empty, Seq(app1, app2, app3))
      deleteDeveloperWillSucceed

      val result = await(underTest.deleteDeveloper(user.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyTeamMemberRemovedFromApp(app1, user.email, gatekeeperUserId, Seq.empty, environment = "SANDBOX")
      verifyTeamMemberRemovedFromApp(app2, user.email, gatekeeperUserId, Seq.empty, environment = "SANDBOX")
      verifyTeamMemberRemovedFromApp(app3, user.email, gatekeeperUserId, Seq.empty, environment = "SANDBOX")

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email)))(*)
    }

    "fail if the developer is the sole admin on any of their associated apps in production" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
      val productionApps = Seq(anApp("productionApplication", Set(Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))))
      val sandboxApps = Seq(anApp(
        name = "sandboxApplication",
        collaborators = Set(Collaborator(developer.email, CollaboratorRole.DEVELOPER), Collaborator("another@example.com", CollaboratorRole.ADMINISTRATOR))
      ))
      fetchDeveloperWillReturn(developer, productionApps, sandboxApps)

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(*[DeleteDeveloperRequest])(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
      verify(mockSandboxApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
    }

    "fail if the developer is the sole admin on any of their associated apps in sandbox" in new Setup {
      val gatekeeperUserId = "gate.keeper"
      val developer = aUser("Fred")
      val productionApps = Seq(anApp(
        name = "productionApplication",
        collaborators = Set(Collaborator(developer.email, CollaboratorRole.DEVELOPER), Collaborator("another@example.com", CollaboratorRole.ADMINISTRATOR))
      ))
      val sandboxApps = Seq(anApp("sandboxApplication", Set(Collaborator(developer.email, CollaboratorRole.ADMINISTRATOR))))
      fetchDeveloperWillReturn(developer, productionApps, sandboxApps)

      val result = await(underTest.deleteDeveloper(developer.email, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(*[DeleteDeveloperRequest])(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*[HeaderCarrier])
      verify(mockSandboxApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*[HeaderCarrier])
    }
  }

  "developerService searchDevelopers" should {
    "find users" in new Setup {
      private val user = aUser("fred")
      private val emailFilter = "example"

      when(mockDeveloperConnector.searchDevelopers(*, *)(*)).thenReturn(List(user))

      val filter = Developers2Filter(maybeEmailFilter = Some(emailFilter))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user)

      verify(mockDeveloperConnector).searchDevelopers(Some(emailFilter), DeveloperStatusFilter.AllStatus)
    }

    "find by api context and version" in new Setup {
      val user1 = aUser("production")
      val user2 = aUser("sandbox")

      private val email1 = user1.email
      private val email2 = user2.email

      when(mockProductionApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(user1.email))
      when(mockSandboxApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(user2.email))

      when(mockDeveloperConnector.fetchByEmails(Set(email1, email2))).thenReturn(Seq(user1, user2))

      val filter = Developers2Filter(environmentFilter = AnyEnvironment, maybeApiFilter = Some(ApiContextVersion("api", "1.0")))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)

      verify(mockProductionApplicationConnector).searchCollaborators("api", "1.0", None)
      verify(mockSandboxApplicationConnector).searchCollaborators("api", "1.0", None)
    }

    "find by api context and version where same email in production and sandbox" in new Setup {
      val user = aUser("user")

      when(mockProductionApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(user.email))
      when(mockSandboxApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(user.email))

      when(mockDeveloperConnector.fetchByEmails(Set(user.email))).thenReturn(Seq(user))

      val filter = Developers2Filter(maybeApiFilter = Some(ApiContextVersion("api", "1.0")))

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

      when(mockProductionApplicationConnector
        .searchCollaborators(eqTo("api"), eqTo("1.0"), eqTo(Some(emailFilter)))(*))
        .thenReturn(Seq(email1, email2, email3))

      when(mockSandboxApplicationConnector
        .searchCollaborators(eqTo("api"), eqTo("1.0"), eqTo(Some(emailFilter)))(*))
        .thenReturn(Seq.empty[String])

      when(mockDeveloperConnector.fetchByEmails(Set(email1, email2, email3))).thenReturn(Seq(user1, user2))

      val filter = Developers2Filter(maybeEmailFilter = Some(emailFilter), maybeApiFilter = Some(ApiContextVersion("api", "1.0")))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)
    }

    "find by developer status" in new Setup {

      val user1 = aUser("user1")

      val filter = Developers2Filter(None, None, developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)

      when(mockDeveloperConnector.searchDevelopers(None, DeveloperStatusFilter.VerifiedStatus)).thenReturn(List(user1))

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

      when(mockProductionApplicationConnector
        .searchCollaborators(eqTo("api"), eqTo("1.0"), eqTo(Some(emailFilter)))(*))
        .thenReturn(Seq(email1, email2, email3, email4))

      when(mockSandboxApplicationConnector
        .searchCollaborators(eqTo("api"), eqTo("1.0"), eqTo(Some(emailFilter)))(*))
        .thenReturn(Seq.empty[String])

      when(mockDeveloperConnector.fetchByEmails(*) (*)).thenReturn(Seq(user1, user2, user3))

      val filter = Developers2Filter(maybeEmailFilter =
        Some(emailFilter), maybeApiFilter = Some(ApiContextVersion("api", "1.0")), developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)

      verify(mockDeveloperConnector).fetchByEmails(Set(email1, email2, email3, email4))
    }

    "find by developer status should sort users by email" in new Setup {

      val firstInTheListUser = User("101@example.com", "alphaFirstName", "alphaLastName", Some(true))
      val secondInTheListUser = User("lalala@example.com", "betaFirstName", "betaLastName", Some(false))
      val thirdInTheListUser = User("zigzag@example.com", "thetaFirstName", "thetaLastName", Some(false))

      val filter = Developers2Filter(None, None, developerStatusFilter = DeveloperStatusFilter.AllStatus)

      when(mockDeveloperConnector.searchDevelopers(None, DeveloperStatusFilter.AllStatus)).thenReturn(List(thirdInTheListUser, firstInTheListUser, secondInTheListUser))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(firstInTheListUser, secondInTheListUser, thirdInTheListUser)

      verify(mockDeveloperConnector).searchDevelopers(None, DeveloperStatusFilter.AllStatus)
    }

    "find by api context and version and Production environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser = aUser("sandbox")

      private val email1 = productionUser.email

      when(mockProductionApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(productionUser.email))
      when(mockSandboxApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(sandboxUser.email))

      when(mockDeveloperConnector.fetchByEmails(Set(email1))).thenReturn(Seq(productionUser))

      val filter = Developers2Filter(environmentFilter = ProductionEnvironment, maybeApiFilter = Some(ApiContextVersion("api", "1.0")))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(productionUser)

      verify(mockProductionApplicationConnector).searchCollaborators("api", "1.0", None)
      verify(mockSandboxApplicationConnector, never).searchCollaborators(*, *, *)(*)
    }

    "find by api context and version and Sandbox environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser = aUser("sandbox")

      private val email2 = sandboxUser.email

      when(mockProductionApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(productionUser.email))
      when(mockSandboxApplicationConnector.searchCollaborators(*, *, *)(*))
        .thenReturn(Seq(sandboxUser.email))

      when(mockDeveloperConnector.fetchByEmails(Set(email2))).thenReturn(Seq(sandboxUser))

      val filter = Developers2Filter(environmentFilter = SandboxEnvironment, maybeApiFilter = Some(ApiContextVersion("api", "1.0")))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(sandboxUser)

      verify(mockProductionApplicationConnector, never).searchCollaborators(*, *, *)(*)
      verify(mockSandboxApplicationConnector).searchCollaborators("api", "1.0", None)
    }
  }
}
