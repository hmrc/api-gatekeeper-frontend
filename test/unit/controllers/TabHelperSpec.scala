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

package unit.controllers

import controllers.TabHelper._
import controllers.{HandleUpliftForm, TabHelper, TabLink}
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class TabHelperSpec extends UnitSpec with Matchers {

  def hrefOfTab(name: String, tabLinks: Seq[TabLink]): String = {
    def tabLinkSelector(tl: TabLink): Boolean = tl.label.equals(name)
    tabLinks.find(tabLinkSelector).get.href
  }

  "TabHelper" should {

    "have developer's tab href set to developers" in {
      hrefOfTab("Developers", dashboardTabs(0)) endsWith "developers"
    }

    "have applications's tab href set to dashboard" in {
      hrefOfTab("Applications", dashboardTabs(0)) endsWith "dashboard"
    }
  }
}
