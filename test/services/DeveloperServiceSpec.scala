/*
 * Copyright 2021 HM Revenue & Customs
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

import config.AppConfig
import connectors._
import model._
import org.joda.time.DateTime
import org.mockito.invocation.InvocationOnMock
import uk.gov.hmrc.http.HeaderCarrier
import utils.AsyncHmrcSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful
import utils.CollaboratorTracker
import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import mocks.connectors._

class DeveloperServiceSpec extends AsyncHmrcSpec with CollaboratorTracker {

  def aUser(name: String, verified: Boolean = true) = {
    val email = s"$name@example.com"
    RegisteredUser(email, idOf(email), "Fred", "Example", verified)
  }

  def aDeveloper(name: String, apps: List[Application] = List.empty, verified: Boolean = true) = {
    val email = s"$name@example.com"
    Developer(
      RegisteredUser(email, idOf(email), name, s"${name}son", verified),
      apps
      )
  }
      
  def anUnregisteredDeveloper(name: String, apps: List[Application] = List.empty) = {
    val email = s"$name@example.com"
    Developer(
      UnregisteredUser(email, idOf(email)),
      apps
      )
  }

  def anApp(name: String, collaborators: Set[Collaborator], deployedTo: String = "PRODUCTION"): ApplicationResponse = {
    ApplicationResponse(ApplicationId.random, ClientId("clientId"), "gatewayId", name, deployedTo, None, collaborators, DateTime.now(), DateTime.now(), Standard(), ApplicationState())
  }

  def aProdApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = "PRODUCTION")

  def aSandboxApp(name: String, collaborators: Set[Collaborator]): ApplicationResponse = anApp(name, collaborators, deployedTo = "SANDBOX")

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with ApplicationConnectorMockProvider with DeveloperConnectorMockProvider {
    val mockAppConfig = mock[AppConfig]

    val underTest = new DeveloperService(mockAppConfig, mockDeveloperConnector, mockSandboxApplicationConnector, mockProductionApplicationConnector)

    val verifiedAdminUser = aUser("admin1")
    val verifiedAdminCollaborator = verifiedAdminUser.email.asAdministratorCollaborator
    val unverifiedUser = aUser("admin2", verified = false)
    val unverifiedAdminCollaborator = unverifiedUser.email.asAdministratorCollaborator
    val developerUser = aUser("developer1")
    val developerCollaborator = developerUser.email.asDeveloperCollaborator
    val commonUsers = List(verifiedAdminUser, unverifiedUser, developerUser)
    val apiContext = ApiContext("api")
    val apiVersion = ApiVersion.random

    implicit val hc = HeaderCarrier()

    def fetchDeveloperWillReturn(user: RegisteredUser, productionApps: List[ApplicationResponse] = List.empty, sandboxApps: List[ApplicationResponse] = List.empty) = {
      DeveloperConnectorMock.FetchByEmail.handles(user)
      DeveloperConnectorMock.FetchByUserId.handles(user)
      DeveloperConnectorMock.FetchById.handles(user)

      ApplicationConnectorMock.Prod.FetchApplicationsByEmail.returns(productionApps: _*)
      ApplicationConnectorMock.Sandbox.FetchApplicationsByEmail.returns(sandboxApps: _*)
    }

    def fetchDevelopersWillReturnTheRequestedUsers = {
      when(mockDeveloperConnector.fetchByEmails(*)(*)).thenAnswer((invocationOnMock: InvocationOnMock) => {
          val developersRequested = invocationOnMock.getArguments()(0).asInstanceOf[Iterable[String]].toSet
          successful(commonUsers.filter(user => developersRequested.contains(user.email)))
      })
    }

    def deleteDeveloperWillSucceed = {
      when(mockDeveloperConnector.deleteDeveloper(*)(*))
        .thenReturn(successful(DeveloperDeleteSuccessResult))
      ApplicationConnectorMock.Prod.RemoveCollaborator.succeeds()
      ApplicationConnectorMock.Sandbox.RemoveCollaborator.succeeds()
    }

    def verifyCollaboratorRemovedFromApp(app: Application,
                                       userToRemove: String,
                                       gatekeeperUserId: String,
                                       adminsToEmail: List[String],
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

    def removeMfaReturnWillReturn(user: RegisteredUser) = {
      when(mockDeveloperConnector.removeMfa(*, *)(*)).thenReturn(successful(user))
    }
  }

  "developerService" should {

    "filter all users (no unregistered collaborators)" in new Setup {
      val applications = List(
        anApp("application1", Set(
          "Bob@example.com".asAdministratorCollaborator,
          "Jacob@example.com".asDeveloperCollaborator)))

      val bob = aDeveloper("Bob", applications)
      val jim = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob,jim,jacob)

      val result = underTest.filterUsersBy(AllUsers, applications)(users)

      result should contain allElementsOf List(bob, jim, jacob)
    }

    "filter all users (including unregistered collaborators)" in new Setup {
      val applications = List(
        anApp("application1", Set(
          "Bob@example.com".asAdministratorCollaborator,
          "Jacob@example.com".asDeveloperCollaborator)),
        anApp("application2", Set(
          "Julia@example.com".asAdministratorCollaborator,
          "Jim@example.com".asDeveloperCollaborator)))

      val bob = aDeveloper("Bob", applications)
      val jim = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob, jim, jacob)

      val result = underTest.filterUsersBy(AllUsers, applications)(users)
      result should contain allElementsOf List(bob, jim, jacob, anUnregisteredDeveloper("Julia", applications.tail))
    }

    "filter users that have access to 1 or more applications" in new Setup {
      val applications = List(
        anApp("application1", Set(
          "Bob@example.com".asAdministratorCollaborator,
          "Jacob@example.com".asDeveloperCollaborator)),
        anApp("application2", Set(
          "Julia@example.com".asAdministratorCollaborator,
          "Jim@example.com".asDeveloperCollaborator)))

      val bob = aDeveloper("Bob", applications)
      val jim = aDeveloper("Jim", applications)
      val jacob = aDeveloper("Jacob", applications)

      val users = List(bob, jim, jacob)

      val result = underTest.filterUsersBy(OneOrMoreSubscriptions, applications)(users)
      result should contain allElementsOf List(bob, jim, jacob, anUnregisteredDeveloper("Julia", applications.tail))
    }

    "filter users that are not associated with any applications" in
      new Setup {
        val applications = List(
          anApp("application1", Set(
            "Shirley@example.com".asAdministratorCollaborator,
            "Jacob@example.com".asDeveloperCollaborator)),
          anApp("application2", Set(
            "Julia@example.com".asAdministratorCollaborator,
            "Jim@example.com".asDeveloperCollaborator)))

        val gaia = aDeveloper("Gaia")
        val jimbob = aDeveloper("Jimbob")
        val shirley = aDeveloper("Shirley")
        val users = List(shirley, jimbob, gaia)

        val result = underTest.filterUsersBy(NoApplications, applications)(users)
        result should contain allElementsOf List(gaia, jimbob)
      }

    "filter users who have no subscriptions" in new Setup {
      val _allApplications = List(
        anApp("application1", Set(
          "Bob@example.com".asAdministratorCollaborator,
          "Jim@example.com".asDeveloperCollaborator,
          "Jacob@example.com".asDeveloperCollaborator
          )
        ),
        anApp("application2", Set(
          "Julia@example.com".asAdministratorCollaborator,
          "Jim@example.com".asDeveloperCollaborator
          )
        )
      )

      val gaia = aDeveloper("Gaia")
      val jimbob = aDeveloper("Jimbob")
      val shirley = aDeveloper("Shirley")
      val jim = aDeveloper("Jim", _allApplications)
      val users = List(shirley, jimbob, gaia, jim)

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
      val users = List(aDeveloper("Bob", verified = false), aDeveloper("Brian"), aDeveloper("Sheila"))
      val result = underTest.filterUsersBy(AnyStatus)(users)
      result shouldBe users
    }

    "filter by status only returns verified users when Verified status" in new Setup {
      val bob = aDeveloper("Bob", verified = false)
      val brian = aDeveloper("Brian")
      val sheila = aDeveloper("Sheila")
      val users = List(bob, brian, sheila)

      val result = underTest.filterUsersBy(VerifiedStatus)(users)

      result shouldBe List(brian, sheila)
    }

    "filter by status only returns unverified users when Unverified status" in new Setup {
      val bob = aDeveloper("Bob", verified = false)
      val brian = aDeveloper("Brian")
      val sheila = aDeveloper("Sheila")
      val users = List(bob, brian, sheila)
      
      val result = underTest.filterUsersBy(UnverifiedStatus)(users)

      result shouldBe List(bob)
    }

    "fetch the developer and the applications they are a team member on" in new Setup {
      val developer = aUser("Fred")
      val apps = List(anApp("application", Set(developer.email.asAdministratorCollaborator)))
      fetchDeveloperWillReturn(developer, apps)

      val result = await(underTest.fetchDeveloper(developer.email))
      result shouldBe Developer(developer, apps)
      verify(mockDeveloperConnector).fetchById(eqTo(EmailIdentifier(developer.email)))(*)
      verify(mockProductionApplicationConnector).fetchApplicationsByEmail(eqTo(developer.email))(*)
    }

    "remove MFA" in new Setup {
      val developer = aUser("Fred")
      val developerId = UuidIdentifier(developer.userId)
      val loggedInUser: String = "admin-user"
      removeMfaReturnWillReturn(developer)

      val result = await(underTest.removeMfa(developerId, loggedInUser))

      result shouldBe developer
      verify(mockDeveloperConnector).removeMfa(developerId, loggedInUser)
    }
  }

  "fetchDeveloper" should {
    val user = aUser("Fred")

    "fetch the developer when requested by email" in new Setup {
      fetchDeveloperWillReturn(user)

      await(underTest.fetchDeveloper(user.email)) shouldBe Developer(user, List.empty)
    }

    "fetch the developer when requested by userId" in new Setup {
      fetchDeveloperWillReturn(user)

      await(underTest.fetchDeveloper(user.userId)) shouldBe Developer(user, List.empty)
    }

    "fetch the developer when requested by email as developerId" in new Setup {
      fetchDeveloperWillReturn(user)

      await(underTest.fetchDeveloper(EmailIdentifier(user.email))) shouldBe Developer(user, List.empty)
    }

    "fetch the developer when requested by userId as developerId" in new Setup {
      fetchDeveloperWillReturn(user)

      await(underTest.fetchDeveloper(UuidIdentifier(user.userId))) shouldBe Developer(user, List.empty)
    }
  }

  "developerService.deleteDeveloper" should {
    val gatekeeperUserId = "gate.keeper"
    val user = aUser("Fred")
    val developerId = UuidIdentifier(user.userId)

    "delete the developer if they have no associated apps in either sandbox or production" in new Setup {
  
      fetchDeveloperWillReturn(user, productionApps = List.empty, sandboxApps = List.empty)
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email)))(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(*[ApplicationId], *, *, *)(*)
      verify(mockSandboxApplicationConnector, never).removeCollaborator(*[ApplicationId], *, *, *)(*)
    }

    "remove the user from their apps and email other verified admins on each production app before deleting the user" in new Setup {
      val app1 = aProdApp("application1", Set(verifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app2 = aProdApp("application2", Set(unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app3 = aProdApp("application3", Set(verifiedAdminCollaborator, unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))

      fetchDeveloperWillReturn(user, List(app1, app2, app3))
      fetchDevelopersWillReturnTheRequestedUsers
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyCollaboratorRemovedFromApp(app1, user.email, gatekeeperUserId, List(verifiedAdminCollaborator.emailAddress))
      verifyCollaboratorRemovedFromApp(app2, user.email, gatekeeperUserId, List.empty)
      verifyCollaboratorRemovedFromApp(app3, user.email, gatekeeperUserId, List(verifiedAdminCollaborator.emailAddress))

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email)))(*)
    }

    "remove the user from their apps without emailing other verified admins on each sandbox app before deleting the user" in new Setup {
     val app1 = aSandboxApp("application1", Set(verifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app2 = aSandboxApp("application2", Set(unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))
      val app3 = aSandboxApp("application3", Set(verifiedAdminCollaborator, unverifiedAdminCollaborator, user.email.asAdministratorCollaborator))

      fetchDeveloperWillReturn(user, List.empty, List(app1, app2, app3))
      deleteDeveloperWillSucceed

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteSuccessResult

      verifyCollaboratorRemovedFromApp(app1, user.email, gatekeeperUserId, List.empty, environment = "SANDBOX")
      verifyCollaboratorRemovedFromApp(app2, user.email, gatekeeperUserId, List.empty, environment = "SANDBOX")
      verifyCollaboratorRemovedFromApp(app3, user.email, gatekeeperUserId, List.empty, environment = "SANDBOX")

      verify(mockDeveloperConnector).deleteDeveloper(eqTo(DeleteDeveloperRequest(gatekeeperUserId, user.email)))(*)
    }

    "fail if the developer is the sole admin on any of their associated apps in production" in new Setup {
      val productionApps = List(anApp("productionApplication", Set(user.email.asAdministratorCollaborator)))
      val sandboxApps = List(anApp(
        name = "sandboxApplication",
        collaborators = Set(user.email.asDeveloperCollaborator, "another@example.com".asAdministratorCollaborator)
      ))
      fetchDeveloperWillReturn(user, productionApps, sandboxApps)

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(*)(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
      verify(mockSandboxApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
    }

    "fail if the developer is the sole admin on any of their associated apps in sandbox" in new Setup {
      val productionApps = List(anApp(
        name = "productionApplication",
        collaborators = Set(user.email.asDeveloperCollaborator, "another@example.com".asAdministratorCollaborator)
      ))
      val sandboxApps = List(anApp("sandboxApplication", Set(user.email.asAdministratorCollaborator)))

      fetchDeveloperWillReturn(user, productionApps, sandboxApps)

      val (result, _) = await(underTest.deleteDeveloper(developerId, gatekeeperUserId))
      result shouldBe DeveloperDeleteFailureResult

      verify(mockDeveloperConnector, never).deleteDeveloper(*)(*)
      verify(mockProductionApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
      verify(mockSandboxApplicationConnector, never).removeCollaborator(eqTo(ApplicationId("productionApplication")), *, *, *)(*)
    }
  }

  "developerService searchDevelopers" should {
    "find users" in new Setup {
      private val user = aUser("fred")
      private val emailFilter = "example"
      DeveloperConnectorMock.SearchDevelopers.returns(user)

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

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(email1)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(email2)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1, email2))(user1, user2)

      val filter = Developers2Filter(environmentFilter = AnyEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

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

      val filter = Developers2Filter(maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

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
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1, email2, email3))(user1,user2)

      val filter = Developers2Filter(maybeEmailFilter = Some(emailFilter), maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)
    }

    "find by developer status" in new Setup {
      val user1 = aUser("user1")
      DeveloperConnectorMock.SearchDevelopers.returnsFor(None, DeveloperStatusFilter.VerifiedStatus)(user1)
      
      val filter = Developers2Filter(None, None, developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)
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

      val filter = Developers2Filter(maybeEmailFilter =
        Some(emailFilter), maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)), developerStatusFilter = DeveloperStatusFilter.VerifiedStatus)

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(user1, user2)

      verify(mockDeveloperConnector).fetchByEmails(Set(email1, email2, email3, email4))
    }

    "find by developer status should sort users by email" in new Setup {

      val firstInTheListUser = RegisteredUser("101@example.com", UserId.random, "alphaFirstName", "alphaLastName", true)
      val secondInTheListUser = RegisteredUser("lalala@example.com", UserId.random, "betaFirstName", "betaLastName", false)
      val thirdInTheListUser = RegisteredUser("zigzag@example.com", UserId.random, "thetaFirstName", "thetaLastName", false)
      DeveloperConnectorMock.SearchDevelopers.returnsFor(None, DeveloperStatusFilter.AllStatus)(thirdInTheListUser, firstInTheListUser, secondInTheListUser)

      val filter = Developers2Filter(None, None, developerStatusFilter = DeveloperStatusFilter.AllStatus)
      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(firstInTheListUser, secondInTheListUser, thirdInTheListUser)

      verify(mockDeveloperConnector).searchDevelopers(None, DeveloperStatusFilter.AllStatus)
    }

    "find by api context and version and Production environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser = aUser("sandbox")

      private val email1 = productionUser.email

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(productionUser.email)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(sandboxUser.email)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email1))(productionUser)

      val filter = Developers2Filter(environmentFilter = ProductionEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(productionUser)

      verify(mockProductionApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
      verify(mockSandboxApplicationConnector, never).searchCollaborators(*[ApiContext], *[ApiVersion], *)(*)
    }

    "find by api context and version and Sandbox environment" in new Setup {
      val productionUser = aUser("production")
      val sandboxUser = aUser("sandbox")

      private val email2 = sandboxUser.email

      ApplicationConnectorMock.Prod.SearchCollaborators.returns(productionUser.email)
      ApplicationConnectorMock.Sandbox.SearchCollaborators.returns(sandboxUser.email)
      DeveloperConnectorMock.FetchByEmails.returnsFor(Set(email2))(sandboxUser)

      val filter = Developers2Filter(environmentFilter = SandboxEnvironment, maybeApiFilter = Some(ApiContextVersion(apiContext, apiVersion)))

      val result = await(underTest.searchDevelopers(filter))

      result shouldBe List(sandboxUser)

      verify(mockProductionApplicationConnector, never).searchCollaborators(*[ApiContext], *[ApiVersion], *)(*)
      verify(mockSandboxApplicationConnector).searchCollaborators(apiContext, apiVersion, None)
    }
  }
   "developerService fetchDevelopersByEmailPreferences" should {
     val topic = TopicOptionChoice.BUSINESS_AND_POLICY
      val sandboxUser = aUser("sandbox")
      val category1 = APICategory("category1")
      val category2 = APICategory("category2")
      val categories = List(category1, category2)
      val apiName1 = "apiName1"
      val apiName2 = "apiName2"
      val apiName3 = "apiName3"
      val apis = List(apiName1, apiName2, apiName3)

     "call the connector correctly when only passed a topic" in new Setup {
        DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, None, None)(sandboxUser)

        val result = await(underTest.fetchDevelopersByEmailPreferences(topic))
        
        result shouldBe List(sandboxUser)
        
        verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), *, *)(*)
     }

      "call the connector correctly when only passed a topic and a category" in new Setup {
        DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, None, Some(Seq(category1)))(sandboxUser)

        val result = await(underTest.fetchDevelopersByAPICategoryEmailPreferences(topic, category1))
        
        result shouldBe List(sandboxUser)
        
        verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), *, eqTo(Some(List(category1))))(*)
     }

     "call the connector correctly passed a topic, a seqeuence of categories and apis" in new Setup {
        DeveloperConnectorMock.FetchByEmailPreferences.returnsFor(topic, Some(apis), Some(categories))(sandboxUser)

        val result = await(underTest.fetchDevelopersBySpecificAPIEmailPreferences(topic, categories, apis))
        
        result shouldBe List(sandboxUser)
        
        verify(mockDeveloperConnector).fetchByEmailPreferences(eqTo(topic), eqTo(Some(apis)), eqTo(Some(categories)))(*)
     }


   }
}
