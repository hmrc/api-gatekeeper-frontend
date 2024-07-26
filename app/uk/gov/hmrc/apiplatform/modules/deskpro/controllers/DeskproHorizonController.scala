/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.controllers

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.concurrent.Future.successful

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}

import uk.gov.hmrc.apiplatform.modules.deskpro.connectors.DeskproHorizonConnector
import uk.gov.hmrc.apiplatform.modules.deskpro.models.{AddOrganisationForm, AddPersonForm, DeskproOrganisationsResponse, DeskproPeopleResponse}
import uk.gov.hmrc.apiplatform.modules.deskpro.views.html.DeskproHorizonView
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.apiplatform.modules.deskpro.models.AddMembershipForm
import uk.gov.hmrc.apiplatform.modules.deskpro.services.DeskproHorizonService
import uk.gov.hmrc.apiplatform.modules.deskpro.models.ViewMembershipForm

@Singleton
class DeskproHorizonController @Inject() (
    connector: DeskproHorizonConnector,
    service: DeskproHorizonService,
    mcc: MessagesControllerComponents,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService,
    deskproHorizonView: DeskproHorizonView
  )(implicit override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions {

  def page() = anyAuthenticatedUserAction { implicit request =>
    successful(Ok(deskproHorizonView(AddOrganisationForm.form, AddPersonForm.form, AddMembershipForm.form, ViewMembershipForm.form)))
  }

  def getOrganisations(full: Boolean): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    connector.getOrganisations().map(response => Ok(if (full) Json.parse(response.body) else Json.toJson(response.json.as[DeskproOrganisationsResponse])))
  }

  def createOrganisation(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    AddOrganisationForm.form.bindFromRequest().fold(
      formWithErrors => {
        successful(BadRequest(deskproHorizonView(formWithErrors, AddPersonForm.form, AddMembershipForm.form, ViewMembershipForm.form)))
      },
      formData => connector.createOrganisation(formData.name).map(response => Ok(Json.parse(response.body)))
    )
  }

  def getPeople(full: Boolean): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    connector.getPeople().map(response => Ok(if (full) Json.parse(response.body) else Json.toJson(response.json.as[DeskproPeopleResponse])))
  }

  def getPerson(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    connector.getPerson("petey@example.com").map {
      case Some(person) => Ok(Json.toJson(person))
      case None => BadRequest(deskproHorizonView(AddOrganisationForm.form, AddPersonForm.form, AddMembershipForm.form, ViewMembershipForm.form))
    }
  }

  def createPerson(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    AddPersonForm.form.bindFromRequest().fold(
      formWithErrors => {
        successful(BadRequest(deskproHorizonView(AddOrganisationForm.form, formWithErrors, AddMembershipForm.form, ViewMembershipForm.form)))
      },
      formData => connector.createPerson(formData.name, formData.email).map(response => Ok(Json.parse(response.body)))
    )
  }

  def createMembership(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    AddMembershipForm.form.bindFromRequest().fold(
      formWithErrors => {
        successful(BadRequest(deskproHorizonView(AddOrganisationForm.form, AddPersonForm.form, formWithErrors, ViewMembershipForm.form)))
      },
      formData => {
        service.addMembership(formData.orgId, formData.email).map(response => Ok(Json.toJson(response)))
      }
    )
  }

  def getMembers(): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    ViewMembershipForm.form.bindFromRequest().fold(
      formWithErrors => {
        successful(BadRequest(deskproHorizonView(AddOrganisationForm.form, AddPersonForm.form, AddMembershipForm.form, formWithErrors)))
      },
      formData => {
        service.getMembers(formData.orgId).map(response => Ok(Json.toJson(response)))
      }
    )
  }
}
