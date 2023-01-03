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

package uk.gov.hmrc.gatekeeper.controllers.apicataloguepublish

import uk.gov.hmrc.gatekeeper.views.html.ForbiddenView
import uk.gov.hmrc.gatekeeper.views.html.apicataloguepublish.PublishTemplate

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import org.jsoup.Jsoup
import play.api.test.FakeRequest
import uk.gov.hmrc.gatekeeper.utils.FakeRequestCSRFSupport._
import play.filters.csrf.CSRF
import uk.gov.hmrc.gatekeeper.utils.WithCSRFAddToken
import uk.gov.hmrc.gatekeeper.controllers.ControllerBaseSpec
import uk.gov.hmrc.gatekeeper.controllers.ControllerSetupBase

import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles

class ApiCataloguePublishControllerSpec extends ControllerBaseSpec with WithCSRFAddToken {

  implicit val materializer = app.materializer

  trait Setup extends ControllerSetupBase {

    val csrfToken = "csrfToken" -> app.injector.instanceOf[CSRF.TokenProvider].generateToken

    implicit val fakeRequest = FakeRequest().withCSRFToken

    private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]

    private lazy val publishTemplateView: PublishTemplate = app.injector.instanceOf[PublishTemplate]

    val controller = new ApiCataloguePublishController(
      StrideAuthorisationServiceMock.aMock,
      mockApiCataloguePublishConnector,
      forbiddenView,
      mcc,
      publishTemplateView
    )
  }

  "ApiCataloguePublishController" when {

    "/apicatalogue/start" should {

      "return startpage when logged in as Admin" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        val result = controller.start()(fakeRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish Page"
      }

      "return forbidden page when logged in as normal user " in new Setup {
        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        val result = controller.start()(fakeRequest)
        status(result) shouldBe FORBIDDEN
        contentAsString(result)

        verifyZeroInteractions(mockApiCataloguePublishConnector)
      }
    }

    "/apicatalogue/publish-all" should {
      "return forbidden page when logged in as standard user" in new Setup {

        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments
        val result = controller.publishAll()(fakeRequest)
        status(result) shouldBe FORBIDDEN
        contentAsString(result)

        verifyZeroInteractions(mockApiCataloguePublishConnector)
      }

      "return publish template with success message when logged in as Admin and connector returns a Right" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)
        ApiCataloguePublishConnectorMock.PublishAll.returnRight
        val result = controller.publishAll()(fakeRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish Page"
        document.getElementById("message").text() shouldBe "Publish All Called ok - Publish all called and is working in the background, check application logs for progress"
      }

      "return publish template with failure message when logged in as Admin and connector returns a Left" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApiCataloguePublishConnectorMock.PublishAll.returnLeft
        val result = controller.publishAll()(fakeRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish All Failed"
        document.getElementById("message").text() shouldBe "Something went wrong with publish all"
      }

    }

    "/apicatalogue/publish?serviceName=" should {
      "return forbidden page when logged in as normal user " in new Setup {

        StrideAuthorisationServiceMock.Auth.hasInsufficientEnrolments

        val result = controller.publishByServiceName("serviceName")(fakeRequest)
        status(result) shouldBe FORBIDDEN
        contentAsString(result)

        verifyZeroInteractions(mockApiCataloguePublishConnector)
      }

      "return publish template with success message when logged in as Admin and connector returns a Right" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApiCataloguePublishConnectorMock.PublishByServiceName.returnRight
        val result = controller.publishByServiceName("serviceName")(fakeRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish Page"
        document.getElementById(
          "message"
        ).text() shouldBe """Publish by serviceName called ok serviceName - {"id":"id","publisherReference":"publisherReference","platformType":"platformType"}"""

        verify(mockApiCataloguePublishConnector).publishByServiceName(eqTo("serviceName"))(*)
      }

      "return publish template with failure message when logged in as Admin and connector returns a Left" in new Setup {
        StrideAuthorisationServiceMock.Auth.succeeds(GatekeeperRoles.ADMIN)

        ApiCataloguePublishConnectorMock.PublishByServiceName.returnLeft
        val result = controller.publishByServiceName("serviceName")(fakeRequest)
        status(result) shouldBe OK

        val document = Jsoup.parse(contentAsString(result))
        document.getElementById("heading").text() shouldBe "Publish by ServiceName failed"
        document.getElementById("message").text() shouldBe """Something went wrong with publish by serviceName serviceName"""

        verify(mockApiCataloguePublishConnector).publishByServiceName(eqTo("serviceName"))(*)
      }
    }

  }

}
