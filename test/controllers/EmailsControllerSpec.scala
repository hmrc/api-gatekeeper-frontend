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

package controllers

import akka.stream.Materializer
import model.User
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import uk.gov.hmrc.http.HeaderCarrier
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.emails.{EmailAllUsersView, EmailApiSubscriptionsView, EmailInformationView, SendEmailChoiceView}
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailsControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer: Materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val sendEmailChoiceView = app.injector.instanceOf[SendEmailChoiceView]
  private lazy val emailInformationView = app.injector.instanceOf[EmailInformationView]
  private lazy val emailAllUsersView = app.injector.instanceOf[EmailAllUsersView]
  private lazy val emailApiSubscriptionsView = app.injector.instanceOf[EmailApiSubscriptionsView]


  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken: (String, String) = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken
      val mockDeveloperService: DeveloperService = mock[DeveloperService]
      val verifiedUser1: User = User("user1@hmrc.com", "verifiedUserA", "1", Some(true))
      val verifiedUser2: User = User("user2@hmrc.com", "verifiedUserB", "2", Some(true))
      val verifiedUser3: User = User("user3@hmrc.com", "verifiedUserC", "3", Some(true))
      val unVerifiedUser1: User = User("user1@somecompany.com", "unVerifiedUserA", "1", Some(false))


      def givenVerifiedDeveloper(): Unit = {
        val users = Seq(verifiedUser1, verifiedUser2 )
        when(mockDeveloperService.fetchUsers(any[HeaderCarrier])).thenReturn(Future.successful(users))
      }

      def given3VerifiedDevelopers1Unverified(): Unit = {
        val users = Seq(verifiedUser1, verifiedUser2, verifiedUser3, unVerifiedUser1)
        when(mockDeveloperService.fetchUsers(any[HeaderCarrier])).thenReturn(Future.successful(users))
      }

      def givenNoVerifiedDevelopers(): Unit = {
        val users = Seq(unVerifiedUser1)
        when(mockDeveloperService.fetchUsers(any[HeaderCarrier])).thenReturn(Future.successful(users))
      }

      val underTest = new EmailsController(
        mockDeveloperService,
        mockApiDefinitionService,
        sendEmailChoiceView,
        emailInformationView,
        emailAllUsersView,
        emailApiSubscriptionsView,
        mockApplicationService,
        forbiddenView,
        mockAuthConnector,
        mcc,
        errorTemplateView
      )

    }

    "email landing page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.landing()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Send emails to users based on"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Send emails to users based on</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")

        responseBody should include(raw"""<input id="EMAIL_PREFERENCES" name="sendEmailChoice" aria-label="Email users based on their preferences" type="radio" value="EMAIL_PREFERENCES" disabled>""".stripMargin)

        responseBody should include(raw"""<input id="API_SUBSCRIPTION" name="sendEmailChoice" aria-label="Email users mandatory information about APIs they subscribe to" type="radio" value="API_SUBSCRIPTION" checked>""".stripMargin)

        responseBody should include(raw"""<div class="multiple-choice">Or</div>""")

        responseBody should include(raw"""<input id="EMAIL_ALL_USERS" name="sendEmailChoice" aria-label="Email all users with a Developer Hub account" type="radio" value="EMAIL_ALL_USERS">""".stripMargin)

        verifyAuthConnectorCalledForUser

      }
    }

    "email information page" should {
      "on request with 'all-users' in uri path should render correctly" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("all-users")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Check you can send your email"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 class=\"heading-large\">Check you can email all users</h1>")
        responseBody should include("<li>important notices and service updates</li>")
        responseBody should include("<li>changes to any application they have</li>")
        responseBody should include("<li>making their application accessible</li>")
        verifyAuthConnectorCalledForUser

      }

      "on request with 'api-subscription' in uri path should render correctly" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("api-subscription")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Check you can send your email"
        val responseBody: String = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1 class=\"heading-large\">Check you can send your email</h1>")
        responseBody should include("<li>important notices and service updates</li>")
        responseBody should include("<li>changes to any application they have</li>")
        responseBody should include("<li>making their application accessible</li>")
        verifyAuthConnectorCalledForUser

      }
    }

    "email all Users page" should {
      "on request should render correctly when 3 verified users are retrieved from developer service " in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        given3VerifiedDevelopers1Unverified()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1>Email all users</h1></div>")
        responseBody should include("<div>3 results</div>")

        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>")

        responseBody should include(raw"""<td id="dev-email-0" width="45%">${verifiedUser1.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-0">${verifiedUser1.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-0">${verifiedUser1.lastName}</td>""")

        responseBody should include(raw"""<td id="dev-email-1" width="45%">${verifiedUser2.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-1">${verifiedUser2.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-1">${verifiedUser2.lastName}</td>""")

        responseBody should include(raw"""<td id="dev-email-2" width="45%">${verifiedUser3.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-2">${verifiedUser3.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-2">${verifiedUser3.lastName}</td>""")
        verifyAuthConnectorCalledForUser

      }

      "on request should render correctly when 2 users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenVerifiedDeveloper()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1>Email all users</h1></div>")
        responseBody should include("<div>2 results</div>")

        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>")
        responseBody should include("<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>")

        responseBody should include(raw"""<td id="dev-email-0" width="45%">${verifiedUser1.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-0">${verifiedUser1.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-0">${verifiedUser1.lastName}</td>""")

        responseBody should include(raw"""<td id="dev-email-1" width="45%">${verifiedUser2.email}</td>""")
        responseBody should include(raw"""<td id="dev-fn-1">${verifiedUser2.firstName}</td>""")
        responseBody should include(raw"""<td id="dev-sn-1">${verifiedUser2.lastName}</td>""")
        verifyAuthConnectorCalledForUser

      }

      "on request should render correctly when no verified users are retrieved from the developer service" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        givenNoVerifiedDevelopers()
        val eventualResult: Future[Result] = underTest.emailAllUsersPage()(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Emails all users"
        val responseBody: String = Helpers.contentAsString(eventualResult)

        responseBody should include("<div><h1>Email all users</h1></div>")
        responseBody should include("<div>0 results</div>")

        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">Email</th>"
        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">First name</th>"
        responseBody should not include "<th tabindex=\"0\" class=\"sorting_left-aligned\">Last name</th>"

        verifyAuthConnectorCalledForUser

      }
    }


  }
}
