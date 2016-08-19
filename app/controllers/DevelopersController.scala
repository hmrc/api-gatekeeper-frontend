/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.{ApiDefinitionConnector, ApplicationConnector, AuthConnector, DeveloperConnector}
import services.DeveloperService
import model._
import model.Forms._
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers
import services.DeveloperService

import scala.concurrent.Future

object DevelopersController extends DevelopersController {
  override val developerService: DeveloperService = DeveloperService
  override val apiDefinitionConnector: ApiDefinitionConnector = ApiDefinitionConnector
  override def authConnector = AuthConnector
  override def authProvider = GatekeeperAuthProvider
}


trait DevelopersController extends FrontendController with GatekeeperAuthWrapper {

  val developerService: DeveloperService
  val apiDefinitionConnector: ApiDefinitionConnector

  private def redirect(filter: Option[String], pageNumber: Int, pageSize: Int) = {
    val pageParams = Map(
      "pageNumber" -> Seq(pageNumber.toString),
      "pageSize" -> Seq(pageSize.toString)
    )

    val queryParams = filter.fold(pageParams) { flt: String => Map("filter" -> Seq(flt)) }
    Redirect("", queryParams, 303)
  }

  def developersPage(filter: Option[String], pageNumber: Int, pageSize: Int) = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      for {
        apps <- developerService.filteredApps(filter)
        devs <- developerService.fetchDevelopers
        apis <- apiDefinitionConnector.fetchAll
        users = developerService.getApplicationUsers(devs, apps)
        emails = developerService.emailList(users)
        page = PageableCollection(users, pageNumber, pageSize)
      } yield {
        if (page.valid) Ok(developers(page, emails, apis, filter))
        else redirect(filter, 1, pageSize)
      }
  }

  def submitDeveloperFilter = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val form = developerFilterForm.bindFromRequest.get
      Future.successful(redirect(Option(form.filter), form.pageNumber, form.pageSize))
    }
}
