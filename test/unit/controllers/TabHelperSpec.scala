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

package unit.controllers

import controllers.TabHelper._
import controllers.TabLink
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class TabHelperSpec extends UnitSpec with Matchers {

  def hrefOfTab(name: String, tabLinks: Seq[TabLink]): String = {
    def tabLinkSelector(tl: TabLink): Boolean = tl.label.equals(name)
    tabLinks.find(tabLinkSelector).get.href
  }

  "TabHelper" should {

    "have developers' tab href set to developers" in {
      hrefOfTab("Developers", dashboardTabs(0)) should endWith("developers")
    }

    "have applications' tab href set to applications" in {
      hrefOfTab("Applications", dashboardTabs(0)) should endWith("applications")
    }

    "have dashboard's tab href set to dashboard" in {
      hrefOfTab("Dashboard", dashboardTabs(0)) should endWith("dashboard")
    }
  }
}
