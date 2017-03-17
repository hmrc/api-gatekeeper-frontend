/*
 * Copyright 2017 HM Revenue & Customs
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

package model

import config.AppConfig
import controllers.routes
import play.api.libs.json.Json

case class NavLink(label: String, href: Option[String])

object NavLink {
  implicit val format = Json.format[NavLink]
}

case object StaticNavLinks {

  def apply(implicit appConfig: AppConfig): Seq[NavLink] = {
    val dashboardLink = appConfig.isExternalTestEnvironment match {
      case true => None
      case false => Some(NavLink("Dashboard", Some(routes.DashboardController.dashboardPage().url)))
    }

    dashboardLink.toList ++ Seq(
      NavLink("Applications", Some(routes.ApplicationController.applicationsPage().url)),
      NavLink("Developers", Some(routes.DevelopersController.developersPage(None, None).url)),
      NavLink("API Approvals", Some("/api-service-approval/pending")),
      NavLink("Privileged Access", Some("/privileged-access/application")))
  }
}

case object UserNavLinks {

  private def loggedInNavLinks(userFullName: String) = Seq(
    NavLink(userFullName, None),
    NavLink("Sign out", Some(routes.AccountController.logout().url)))

  private val loggedOutNavLinks = Seq(
    NavLink("Sign in", Some(routes.AccountController.loginPage().url)))

  def apply(userFullName: Option[String]): Seq[NavLink] = userFullName match {
    case Some(name) => loggedInNavLinks(name)
    case None => loggedOutNavLinks
  }
}
