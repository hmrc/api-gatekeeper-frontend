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

package controllers

import model._
import org.mockito.BDDMockito._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import play.filters.csrf.CSRF.TokenProvider
import services.SubscriptionFieldsService
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.applications._
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import utils.CollaboratorTracker

class TeamMembersControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker with MockitoSugar with ArgumentMatchersSugar with CollaboratorTracker {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]
  private lazy val addTeamMemberView = app.injector.instanceOf[AddTeamMemberView]
  private lazy val removeTeamMemberView = app.injector.instanceOf[RemoveTeamMemberView]
      
   running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin()))), List.empty)

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))), List.empty)

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))), List.empty)
      val mockSubscriptionFieldsService = mock[SubscriptionFieldsService]

      val developers = List[RegisteredUser] {
        new RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false)
      }

      val underTest = new TeamMembersController(
        mockDeveloperService,
        mcc,
        manageTeamMembersView,
        addTeamMemberView,
        removeTeamMemberView,
        mockApplicationService,
        mockApmService,
        errorTemplateView,
        forbiddenView,
        mockAuthConnector
      )
    }

    "manageTeamMembers" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            // The auth connector checks you are logged on. And the controller checks you are also a super user as it's a privileged app.
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK

            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
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
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val result = await(addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest))

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val result = await(addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest))

            status(result) shouldBe OK
            verifyAuthConnectorCalledForUser
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
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

          "call the service to add the team member when existing registered" in new Setup {
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *)(*))
              .willReturn(Future.successful(()))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            verify(mockApplicationService)
              .addTeamMember(eqTo(application.application), eqTo(email.asDeveloperCollaborator))(*)
            verifyAuthConnectorCalledForUser
          }

          "redirect back to manageTeamMembers when the service call is successful" in new Setup {
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *)(*))
              .willReturn(Future.successful(()))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
            verifyAuthConnectorCalledForUser
          }

          "show 400 BadRequest when the service call fails with TeamMemberAlreadyExists" in new Setup {
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *)(*))
              .willReturn(Future.failed(new TeamMemberAlreadyExists))

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email), ("role", role))
            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }
        }

        "the form is invalid" should {
          "show 400 BadRequest when the email is invalid" in new Setup {
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", "NOT AN EMAIL ADDRESS"),
              ("role", "DEVELOPER"))

            val result = await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            status(result) shouldBe BAD_REQUEST
          }

          "show 400 BadRequest when the role is invalid" in new Setup {
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
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
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsANormalUser()
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
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsANormalUser()
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
            givenFetchOrCreateUserReturnsRegisteredUser(email)
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()

            given(mockApplicationService.addTeamMember(*, *)(*))
              .willReturn(Future.successful(()))

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
            givenTheGKUserIsAuthorisedAndIsASuperUser()
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
            givenTheGKUserIsAuthorisedAndIsASuperUser()
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
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email))
            val result = await(addToken(underTest.removeTeamMember(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 200 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
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
      val emailToRemove = "email@example.com"

      "the user is a superuser" when {
        "the form is valid" when {
          "the action is not confirmed" should {
            val confirm = "No"

            "redirect back to the manageTeamMembers page" in new Setup {
              givenTheGKUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
              verifyAuthConnectorCalledForUser
            }
          }

          "the action is confirmed" should {
            val confirm = "Yes"

            "call the service with the correct params" in new Setup {
              givenTheGKUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER

              verify(mockApplicationService).removeTeamMember(eqTo(application.application), eqTo(emailToRemove), eqTo("superUserName"))(*)
              verifyAuthConnectorCalledForUser
            }

            "show a 400 Bad Request when the service fails with TeamMemberLastAdmin" in new Setup {
              givenTheGKUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.failed(new TeamMemberLastAdmin))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe BAD_REQUEST
            }

            "redirect to the manageTeamMembers page when the service call is successful" in new Setup {
              givenTheGKUserIsAuthorisedAndIsASuperUser()
              givenTheAppWillBeReturned()

              given(mockApplicationService.removeTeamMember(*, *, *)(*))
                .willReturn(Future.successful(ApplicationUpdateSuccessResult))

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", confirm))
              val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value}/team-members")
              verifyAuthConnectorCalledForUser
            }
          }
        }

        "the form is invalid" should {
          "show 400 Bad Request" in new Setup {
            givenUserExists(emailToRemove)
            givenTheGKUserIsAuthorisedAndIsASuperUser()
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
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing a standard app" should {
          "show 303 OK" in new Setup {
            givenTheGKUserIsAuthorisedAndIsANormalUser()
            givenTheAppWillBeReturned()
            given(mockApplicationService.removeTeamMember(*, *, *)(*))
              .willReturn(Future.successful(ApplicationUpdateSuccessResult))

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove), ("confirm", "Yes"))
            val result = await(addToken(underTest.removeTeamMemberAction(applicationId))(request))

            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }
  }
}