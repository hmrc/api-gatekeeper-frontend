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

import views.html.ForbiddenView
import views.html.ErrorTemplate
import views.html.apicataloguepublish.PublishTemplate

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import utils.FakeRequestCSRFSupport._
import play.filters.csrf.CSRF
import utils.WithCSRFAddToken
import uk.gov.hmrc.auth.core.Enrolment
import mocks.TestRoles._

class ApiCataloguePublishControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = app.materializer

  trait Setup extends ControllerSetupBase {

    val csrfToken = "csrfToken" -> app.injector.instanceOf[CSRF.TokenProvider].generateToken

    override val aLoggedInRequest = FakeRequest().withSession(authToken, userToken).withCSRFToken

    override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken
    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]

    private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

    private lazy val publishTemplateView: PublishTemplate = app.injector.instanceOf[PublishTemplate]

    val controller = new ApiCataloguePublishController(
      mockApiCataloguePublishConnector,
      forbiddenView,
      mockAuthConnector,
      mcc,
      errorTemplateView,
      publishTemplateView
    )
  }

  "ApiCataloguePublishController" when {

    "/apicatalogue/start" should {

      "return startpage when logged in as Admin" in new Setup {

        givenTheGKUserIsAuthorisedAndIsAnAdmin()
        val result = controller.start()(anAdminLoggedInRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish Page"

        verify(mockAuthConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }

      "return forbidden page when logged in as normal user " in new Setup {

        givenTheGKUserHasInsufficientEnrolments()
        val result = controller.start()(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
        contentAsString(result)

        verify(mockAuthConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }
    }

    "/apicatalogue/publish-all" should {
      "return forbidden page when logged in as normal user " in new Setup {

        givenTheGKUserHasInsufficientEnrolments()
        val result = controller.publishAll()(aLoggedInRequest)
        status(result) shouldBe FORBIDDEN
        contentAsString(result)

        verify(mockAuthConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }

      "return publish template with success message when logged in as Admin and connector returns a Right" in new Setup {
        givenTheGKUserIsAuthorisedAndIsAnAdmin()
        ApiCataloguePublishConnectorMock.PublishAll.returnRight
        val result = controller.publishAll()(anAdminLoggedInRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish Page"
        document.getElementById("message").text() shouldBe "Publish All Called ok - Publish all called and is working in the background, check application logs for progress"

        verify(mockAuthConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)

      }

      "return publish template with failure message when logged in as Admin and connector returns a Left" in new Setup {
        givenTheGKUserIsAuthorisedAndIsAnAdmin()
        ApiCataloguePublishConnectorMock.PublishAll.returnLeft
        val result = controller.publishAll()(anAdminLoggedInRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish All Failed"
        document.getElementById("message").text() shouldBe "Something went wrong with publish all"

        verify(mockAuthConnector).authorise(eqTo(Enrolment(adminRole)), *)(*, *)
      }

    }

  }

}
