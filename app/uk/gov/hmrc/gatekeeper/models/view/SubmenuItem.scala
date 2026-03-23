/*
 * Copyright 2026 HM Revenue & Customs
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

import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.controllers.routes

case class SubmenuItem(name: String, href: Option[String])

object SubmenuItem {
  val ApplicationsMenu                       = List(SubmenuItem("Applications", None), SubmenuItem("Terms of use", Some(routes.TermsOfUseController.page().url)))
  def ApiApprovalsMenu(appConfig: AppConfig) = List(SubmenuItem("APIs", Some(appConfig.gatekeeperApisUrl)), SubmenuItem("API approvals", None))
}
