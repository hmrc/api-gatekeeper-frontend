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

import model.EmailOptionChoice
import org.jsoup.Jsoup
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import play.filters.csrf.CSRF.TokenProvider
import services.DeveloperService
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html.emails.{EmailAllUsersView, EmailInformationView, SendEmailChoiceView}
import views.html.{ErrorTemplate, ForbiddenView}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class EmailsControllerSpec extends ControllerBaseSpec with WithCSRFAddToken with TitleChecker {

  implicit val materializer = app.materializer

  private lazy val errorTemplateView = app.injector.instanceOf[ErrorTemplate]
  private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
  private lazy val sendEmailChoiceView = app.injector.instanceOf[SendEmailChoiceView]
  private lazy val emailInformationView = app.injector.instanceOf[EmailInformationView]
  private lazy val emailAllUsersView = app.injector.instanceOf[EmailAllUsersView]



  running(app) {

    trait Setup extends ControllerSetupBase {

      val csrfToken = "csrfToken" -> app.injector.instanceOf[TokenProvider].generateToken
      override val aLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, userToken).withCSRFToken
      override val aSuperUserLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, superUserToken).withCSRFToken
      override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken
      val mockDeveloperService = mock[DeveloperService]

      val underTest = new EmailsController(
        mockDeveloperService,
        sendEmailChoiceView,
        emailInformationView,
        emailAllUsersView,
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
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Send emails to users based on</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        responseBody should include(raw"""<input id="${EmailOptionChoice.EMAIL_PREFERENCES}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.EMAIL_PREFERENCES}" disabled>""")
        responseBody should include(raw"""<input id="${EmailOptionChoice.API_SUBSCRIPTION}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.API_SUBSCRIPTION}" disabled>""")
        responseBody should include(raw"""<div class="multiple-choice">Or</div>""")
        responseBody should include(raw"""<input id="${EmailOptionChoice.EMAIL_ALL_USERS}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.EMAIL_ALL_USERS}" checked>""")

        verifyAuthConnectorCalledForUser

      }
    }

    "email disclaimer page" should {
      "on initial request with logged in user should display disabled options and checked email all options" in new Setup {
        givenTheUserIsAuthorisedAndIsANormalUser()
        val eventualResult: Future[Result] = underTest.showEmailInformation("all_users")(aLoggedInRequest)

        status(eventualResult) shouldBe OK
        titleOf(eventualResult) shouldBe "Unit Test Title - Send emails to users based on"
        val responseBody = Helpers.contentAsString(eventualResult)
        responseBody should include("<h1>Send emails to users based on</h1>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/applications\">Applications</a>")
        responseBody should include("<a class=\"align--middle inline-block \" href=\"/api-gatekeeper/developers2\">Developers</a>")
        responseBody should include(raw"""<input id="${EmailOptionChoice.EMAIL_PREFERENCES}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.EMAIL_PREFERENCES}" disabled>""")
        responseBody should include(raw"""<input id="${EmailOptionChoice.API_SUBSCRIPTION}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.API_SUBSCRIPTION}" disabled>""")
        responseBody should include(raw"""<div class="multiple-choice">Or</div>""")
        responseBody should include(raw"""<input id="${EmailOptionChoice.EMAIL_ALL_USERS}" name="sendEmailChoice" type="radio" value="${EmailOptionChoice.EMAIL_ALL_USERS}" checked>""")

        verifyAuthConnectorCalledForUser

      }
    }

    def assertIncludesOneError(result: Result, message: String) = {

      val body = bodyOf(result)

      body should include(message)
      assert(Jsoup.parse(body).getElementsByClass("form-field--error").size == 1)
    }

  }
}
