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

package uk.gov.hmrc.gatekeeper.models.view

import play.api.libs.json._
import play.api.mvc.RequestHeader

import uk.gov.hmrc.gatekeeper.controllers.routes

case class NavLink(label: String, href: String, active: Boolean, truncate: Boolean, openInNewWindow: Boolean, isSensitive: Boolean)

object NavLink {

  def isActive(url: String)(implicit request: RequestHeader): Boolean = {
    try {
      request.target.path.contains(url.split("/").last)
    } catch {
      case _: Throwable => false
    }
  }

  def apply(label: String, href: String)(implicit request: RequestHeader): NavLink = {
    NavLink(label, href, isActive(href), false, false, false)
  }

  def apply(label: String, href: String, active: Boolean): NavLink = {
    NavLink(label, href, active, false, false, false)
  }

  implicit val format: OFormat[NavLink] = Json.format[NavLink]
}

case object StaticNavLinks {

  def apply(gatekeeperApisUrl: String, gatekeeperTermsOfUseUrl: String, gatekeeperXmlOrganisationsUrl: String)(implicit request: RequestHeader) = {
    Seq(
      NavLink("APIs", gatekeeperApisUrl, false),
      NavLink("Applications", routes.ApplicationController.applicationsPage(None).url),
      NavLink("Developers", routes.DevelopersController.blankDevelopersPage().url),
      NavLink("Terms of use", gatekeeperTermsOfUseUrl, false),
      NavLink("Email", routes.EmailsController.landing().url),
      NavLink("API Approvals", routes.ApiApprovalsController.filterPage().url),
      NavLink("XML", gatekeeperXmlOrganisationsUrl, false)
    )
  }
}
