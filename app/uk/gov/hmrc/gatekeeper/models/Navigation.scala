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

package uk.gov.hmrc.gatekeeper.models

import play.api.libs.json.{Json, OFormat}

import uk.gov.hmrc.gatekeeper.controllers.routes

case class NavLink(label: String, href: Option[String])

object NavLink {
  implicit val format: OFormat[NavLink] = Json.format[NavLink]
}

case object StaticNavLinks {

  def apply(): Seq[NavLink] = {
    Seq(
      NavLink("Applications", Some(routes.ApplicationController.applicationsPage(None).url)),
      NavLink("Developers", Some(routes.DevelopersController.blankDevelopersPage().url)),
      NavLink("Email", Some(routes.EmailsController.landing().url)),
      NavLink("API Approvals", Some(routes.ApiApprovalsController.filterPage().url)),
      NavLink("XML", Some(routes.XmlServicesController.organisationsSearchPage().url))
    )
  }
}

case object UserNavLinks {

  private def loggedInNavLinks(userFullName: String) = Seq(NavLink(userFullName, None))

  private val loggedOutNavLinks: Seq[NavLink] = Seq.empty

  def apply(userFullName: Option[String]): Seq[NavLink] = userFullName match {
    case Some(name) => loggedInNavLinks(name)
    case None       => loggedOutNavLinks
  }
}
