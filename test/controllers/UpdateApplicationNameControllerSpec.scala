/*
 * Copyright 2022 HM Revenue & Customs
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

import builder.{ApiBuilder, ApplicationBuilder}
import config.ErrorHandler
import model._
import model.applications.ApplicationWithSubscriptionData
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.filters.csrf.CSRF.TokenProvider
import services.{ApmService, ApplicationService}
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import utils.FakeRequestCSRFSupport._
import utils.{TitleChecker, WithCSRFAddToken}
import views.html._
import views.html.applications.{ManageApplicationNameAdminListView, ManageApplicationNameSingleAdminView, ManageApplicationNameSuccessView, ManageApplicationNameView, ManageSubscriptionsView}

import scala.concurrent.ExecutionContext.Implicits.global

class UpdateApplicationNameControllerSpec extends ControllerBaseSpec {
      
  implicit val materializer = app.materializer

  val applicationService = mock[ApplicationService]
  val forbiddenView = app.injector.instanceOf[ForbiddenView]
  val errorTemplate = app.injector.instanceOf[ErrorTemplate]
  val manageApplicationNameView = app.injector.instanceOf[ManageApplicationNameView]
  val manageApplicationNameAdminListView = app.injector.instanceOf[ManageApplicationNameAdminListView]
  val manageApplicationNameSingleAdminView = app.injector.instanceOf[ManageApplicationNameSingleAdminView]
  val manageApplicationNameSuccessView = app.injector.instanceOf[ManageApplicationNameSuccessView]
  val apmService = mock[ApmService]
  val errorHandler = app.injector.instanceOf[ErrorHandler]
  val authConnector = mock[AuthConnector]

  trait Setup {
    val underTest = new UpdateApplicationNameController(
      applicationService,
      forbiddenView,
      mcc,
      errorTemplate,
      manageApplicationNameView,
      manageApplicationNameAdminListView,
      manageApplicationNameSingleAdminView,
      manageApplicationNameSuccessView,
      apmService,
      errorHandler,
      authConnector,
      forbiddenHandler
    )
  }

  "updateApplicationNamePage" should {
    "display page correctly" in new Setup {

    }
  }

}
