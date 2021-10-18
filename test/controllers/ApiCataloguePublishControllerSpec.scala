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

class ApiCataloguePublishControllerSpec  extends ControllerBaseSpec {

      implicit val materializer = app.materializer


  trait Setup extends ControllerSetupBase {

val csrfToken = "csrfToken" -> app.injector.instanceOf[CSRF.TokenProvider].generateToken
   override val aLoggedInRequest = FakeRequest().withSession(authToken, userToken).withCSRFToken
   override val anAdminLoggedInRequest = FakeRequest().withSession(csrfToken, authToken, adminToken).withCSRFToken
      private lazy val forbiddenView = app.injector.instanceOf[ForbiddenView]
      private lazy val errorTemplateView: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
      private lazy val publishTemplateView: PublishTemplate = app.injector.instanceOf[PublishTemplate]
      
      val controller = new ApiCataloguePublishController(mockApiCataloguePublishConnector,
      forbiddenView, mockAuthConnector, mcc, errorTemplateView, publishTemplateView)
  }



    "ApiCataloguePublishController" when {

        "/apicatalogue/start" should {
    
            "return startpage when logged in as Admin" in new Setup {

            givenTheGKUserIsAuthorisedAndIsAnAdmin()
              val result =   controller.start()(anAdminLoggedInRequest)
              status(result) shouldBe OK
              
              val document = Jsoup.parse(contentAsString(result))
            }
            "return forbidden page when logged in as normal user " in new Setup {

            givenTheGKUserIsAuthorisedAndIsANormalUser()
              val result =   controller.start()(aLoggedInRequest)
               status(result) shouldBe FORBIDDEN
              contentAsString(result)
            }
        }
    
    }    
  
}
