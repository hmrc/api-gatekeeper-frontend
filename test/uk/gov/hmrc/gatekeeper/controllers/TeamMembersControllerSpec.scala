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

import scala.concurrent.ExecutionContext.Implicits.global

import mocks.services.TeamMemberServiceMockProvider

import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.OverrideFlag.PersistLogin
import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.{Privileged, Ropc, Standard}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, UserId}
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import uk.gov.hmrc.gatekeeper.utils.{CollaboratorTracker, TitleChecker, WithCSRFAddToken}
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

class TeamMembersControllerSpec
    extends ControllerBaseSpec
    with WithCSRFAddToken
    with TitleChecker
    with CollaboratorTracker {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView     = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView         = app.injector.instanceOf[ForbiddenView]
  private lazy val manageTeamMembersView = app.injector.instanceOf[ManageTeamMembersView]
  private lazy val addTeamMemberView     = app.injector.instanceOf[AddTeamMemberView]
  private lazy val removeTeamMemberView  = app.injector.instanceOf[RemoveTeamMemberView]
  private lazy val errorHandler          = app.injector.instanceOf[ErrorHandler]

  running(app) {

    trait Setup extends ControllerSetupBase with TeamMemberServiceMockProvider {

      val csrfToken                          = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest          = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest    = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken

      val applicationWithOverrides = ApplicationWithHistory(
        basicApplication.copy(access = Standard(overrides = Set(PersistLogin))),
        List.empty
      )

      val privilegedApplication = ApplicationWithHistory(
        basicApplication.copy(access = Privileged(scopes = Set("openid", "email"))),
        List.empty
      )

      val ropcApplication = ApplicationWithHistory(
        basicApplication.copy(access = Ropc(scopes = Set("openid", "email"))),
        List.empty
      )

      val developers = List[RegisteredUser] {
        new RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false)
      }

      val underTest = new TeamMembersController(
        mockTeamMemberService,
        mockDeveloperService,
        mcc,
        manageTeamMembersView,
        addTeamMemberView,
        removeTeamMemberView,
        mockApplicationService,
        mockApmService,
        errorTemplateView,
        forbiddenView,
        errorHandler,
        StrideAuthorisationServiceMock.aMock
      )
    }

    "manageTeamMembers" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val result = addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK

            // The auth connector checks you are logged on. And the controller checks you are also a super user as it's a privileged app.
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val result = addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest)

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val result = addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val result = addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest)

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.manageTeamMembers(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.manageTeamMembers(applicationId))(aLoggedInRequest)

            status(result) shouldBe OK
          }
        }
      }
    }

    "update grant length" when {
      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest)

            status(result) shouldBe OK
          }
        }
      }
    }

    "addTeamMember" when {
      "managing a privileged app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val result = addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val result = addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest)

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing an ROPC app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val result = addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val result = addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest)

            status(result) shouldBe FORBIDDEN
          }
        }
      }

      "managing a standard app" when {
        "the user is a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.addTeamMember(applicationId))(aSuperUserLoggedInRequest)

            status(result) shouldBe OK
          }
        }

        "the user is not a superuser" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()

            val result = addToken(underTest.addTeamMember(applicationId))(aLoggedInRequest)

            status(result) shouldBe OK
          }
        }
      }
    }

    "addTeamMemberAction" when {
      val email = "email@example.com".toLaxEmail

      "the user is a superuser" when {
        "the form is valid" should {
          val role = "DEVELOPER"

          "call the service to add the team member when existing registered" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            TeamMemberServiceMock.AddTeamMember.succeeds()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email.text), ("role", role))
            await(addToken(underTest.addTeamMemberAction(applicationId))(request))

            TeamMemberServiceMock.AddTeamMember.verifyCalledWith(application.application, email.asDeveloperCollaborator, Actors.GatekeeperUser("Bobby Example"))
          }

          "redirect back to manageTeamMembers when the service call is successful" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            TeamMemberServiceMock.AddTeamMember.succeeds()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email.text), ("role", role))
            val result  = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe SEE_OTHER
            redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/team-members")
          }

          "show 400 BadRequest when the service call fails with TeamMemberAlreadyExists" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()
            TeamMemberServiceMock.AddTeamMember.failsDueToExistingAlready()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email.text), ("role", role))
            val result  = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe BAD_REQUEST
          }
        }

        "the form is invalid" should {
          "show 400 BadRequest when the email is invalid" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", "NOT AN EMAIL ADDRESS"),
              ("role", "DEVELOPER")
            )

            val result = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe BAD_REQUEST
          }

          "show 400 BadRequest when the role is invalid" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(
              ("email", email.text),
              ("role", "")
            )

            val result = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "manging a privileged app" should {
          "show 403 Forbidden" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email.text),
              ("role", "DEVELOPER")
            )

            val result = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email.text),
              ("role", "DEVELOPER")
            )

            val result = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 303 See Other when valid" in new Setup {
            DeveloperServiceMock.FetchOrCreateUser.handles(email)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()

            TeamMemberServiceMock.AddTeamMember.succeeds()

            val request = aLoggedInRequest.withFormUrlEncodedBody(
              ("email", email.text),
              ("role", "DEVELOPER")
            )

            val result = addToken(underTest.addTeamMemberAction(applicationId))(request)

            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }

    "removeTeamMember" when {
      val email = "email@example.com".toLaxEmail

      "the user is a superuser" when {
        "the form is valid" should {
          "show the remove team member page successfully with the provided email address" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", email.text))
            val result  = addToken(underTest.removeTeamMember(applicationId))(request)

            status(result) shouldBe OK
            contentAsString(result) should include(email.text)
          }
        }

        "the form is invalid" should {
          "show a 400 Bad Request" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result  = addToken(underTest.removeTeamMember(applicationId))(request)

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "managing a privileged app" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email.text))
            val result  = addToken(underTest.removeTeamMember(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(ropcApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email.text))
            val result  = addToken(underTest.removeTeamMember(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "managing a standard app" should {
          "show 200 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", email.text))
            val result  = addToken(underTest.removeTeamMember(applicationId))(request)

            status(result) shouldBe OK
          }
        }
      }
    }

    "removeTeamMemberAction" when {
      val emailToRemove = "email@example.com".toLaxEmail

      "the user is a superuser" when {
        "the form is valid" when {
          "the action is not confirmed" should {
            val confirm = "No"

            "redirect back to the manageTeamMembers page" in new Setup {
              StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
              givenTheAppWillBeReturned()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", confirm))
              val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/team-members")
            }
          }

          "the action is confirmed" should {
            val confirm = "Yes"

            "call the service with the correct params" in new Setup {
              StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
              givenTheAppWillBeReturned()
              TeamMemberServiceMock.RemoveTeamMember.succeeds()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", confirm))
              val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

              status(result) shouldBe SEE_OTHER

              TeamMemberServiceMock.RemoveTeamMember.verifyCalledWith(application.application, emailToRemove, Actors.GatekeeperUser("Bobby Example"))
            }

            "show a 400 Bad Request when the service fails with TeamMemberLastAdmin" in new Setup {
              StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
              givenTheAppWillBeReturned()
              TeamMemberServiceMock.RemoveTeamMember.failsDueToLastAdmin()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", confirm))
              val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

              status(result) shouldBe BAD_REQUEST
            }

            "redirect to the manageTeamMembers page when the service call is successful" in new Setup {
              StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
              givenTheAppWillBeReturned()
              TeamMemberServiceMock.RemoveTeamMember.succeeds()

              val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", confirm))
              val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

              status(result) shouldBe SEE_OTHER
              redirectLocation(result) shouldBe Some(s"/api-gatekeeper/applications/${applicationId.value.toString()}/team-members")
            }
          }
        }

        "the form is invalid" should {
          "show 400 Bad Request" in new Setup {
            DeveloperServiceMock.userExists(emailToRemove)
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.SUPERUSER)
            givenTheAppWillBeReturned()

            val request = aSuperUserLoggedInRequest.withFormUrlEncodedBody(("email", "NOT AN EMAIL ADDRESS"))
            val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

            status(result) shouldBe BAD_REQUEST
          }
        }
      }

      "the user is not a superuser" when {
        "when managing a privileged app" should {
          "show 403 forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", "Yes"))
            val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing an ROPC app" should {
          "show 403 Forbidden" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            ApplicationServiceMock.FetchApplication.returns(privilegedApplication)

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", "Yes"))
            val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

            status(result) shouldBe FORBIDDEN
          }
        }

        "when managing a standard app" should {
          "show 303 OK" in new Setup {
            StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.USER)
            givenTheAppWillBeReturned()
            TeamMemberServiceMock.RemoveTeamMember.succeeds()

            val request = aLoggedInRequest.withFormUrlEncodedBody(("email", emailToRemove.text), ("confirm", "Yes"))
            val result  = addToken(underTest.removeTeamMemberAction(applicationId))(request)

            status(result) shouldBe SEE_OTHER
          }
        }
      }
    }
  }
}
