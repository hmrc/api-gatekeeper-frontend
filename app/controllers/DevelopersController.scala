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

import connectors.{ApiDefinitionConnector, AuthConnector}
import model.APIStatus.APIStatus
import model.Forms._
import model._
import services.DeveloperService
import uk.gov.hmrc.play.frontend.controller.FrontendController
import utils.{GatekeeperAuthProvider, GatekeeperAuthWrapper}
import views.html.developers.developers

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

<<<<<<< HEAD
    val actualFilter = filter match {
      case Some(x) => if (x == "") None else Some(x)
      case None => None
    }

    val queryParams = actualFilter.fold(pageParams) { flt: String => Map("filter" -> Seq(flt)) }
=======
    val filterParams = filter match {
      case Some("") | None => Map.empty
      case Some(flt) => Map("filter" -> Seq(flt))
    }

    val queryParams = pageParams ++ filterParams
>>>>>>> b8fa63e9de284ac7a84cc2458be375293a9c763b
    Redirect("", queryParams, 303)
  }


  private def groupApisByStatus(apis: Seq[APIDefinition]): Map[APIStatus, Seq[VersionSummary]] = {
    
    val versions = for {
      api <- apis
      version <- api.versions
    } yield VersionSummary(api.name, version.status, APIIdentifier(api.context, version.version))

    versions.groupBy(_.status)
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
        if (page.valid) {
<<<<<<< HEAD
          Ok(developers(page, emails, apis, filter))
=======
          Ok(developers(page, emails, groupApisByStatus(apis), filter))
>>>>>>> b8fa63e9de284ac7a84cc2458be375293a9c763b
        }
        else {
          redirect(filter, 1, pageSize)
        }
      }
  }

  def submitDeveloperFilter = requiresRole(Role.APIGatekeeper) {
    implicit request => implicit hc =>
      val form = developerFilterForm.bindFromRequest.get
      Future.successful(redirect(Option(form.filter), form.pageNumber, form.pageSize))
    }
}
