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

package controllers

import config.AppConfig
import play.api.libs.json.Json

case class NavLink(label: String, href: String, truncate: Boolean = false)

object NavLink {
  implicit val format = Json.format[NavLink]
}

object NavigationHelper {

  val nameDisplayLimit = AppConfig.nameDisplayLimit

  def loggedInNavLinks(userFullName: String) = Seq(
    NavLink(userFullName, "#", truncate = true),
    NavLink("Sign out", routes.AccountController.logout.toString)
  )

  val loggedOutNavLinks = Seq(
    NavLink("Sign in", routes.AccountController.loginPage.toString)
  )

  def navLinks(userFullName: Option[String]) = userFullName match {
    case Some(name) => loggedInNavLinks(name)
    case None => loggedOutNavLinks
  }

  val truncate: String => String = {
    s => if (s.length > nameDisplayLimit) s"${s.take(nameDisplayLimit - 3)}..." else s
  }

}
