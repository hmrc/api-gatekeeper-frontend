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

case class TabLink(label: String, href: String, active: Boolean = false)

object TabHelper {
  def dashboardTabs(activeTab: Int) = Seq(
    TabLink("Dashboard", routes.DashboardController.dashboardPage.url, activeTab == 0),
    TabLink("Applications", routes.ApplicationController.applicationsPage.url, activeTab == 1),
    TabLink("Developers", routes.DevelopersController.developersPage(None, None).url, activeTab == 2))
}
