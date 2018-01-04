/*
 * Copyright 2018 HM Revenue & Customs
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

package acceptance.specs

import acceptance.pages.{ApprovedPage, DashboardPage}
import acceptance.ApprovedBaseSpec
import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest._
import play.api.{Application, Mode}
import play.api.inject.guice.GuiceApplicationBuilder

class APIGatekeeperSuperUserApprovedSpec extends ApprovedBaseSpec {

  override def newAppForTest(testData: TestData): Application = {
    GuiceApplicationBuilder()
      .configure("run.mode" -> "Stub", "Stub.superUsers" -> Seq("joe.test"))
      .in(Mode.Prod)
      .build()
  }

  feature("View approved application details") {

    scenario("View current application rate limit details if the logged-in user is a super user", Tag("NonSandboxTest")) {
      stubApplicationListAndDevelopers()
      stubFor(get(urlEqualTo(s"/gatekeeper/application/$approvedApp1"))
        .willReturn(aResponse().withBody(approvedApplication("application description", true)).withStatus(200)))

      signInGatekeeper
      on(DashboardPage)
      clickOnLink(s"data-view-$approvedApp1")
      on(ApprovedPage(approvedApp1, "Application"))
      assertApplicationRateLimitTier(isSuperUser = true, "BRONZE")

      stubApplicationListAndDevelopers()
      stubRateLimitTier("df0c32b6-bbb7-46eb-ba50-e6e5459162ff", "SILVER")
      clickOnElement("rate-limit-tier")
      clickOnElement("SILVER")
      clickOnElement("rate-limit-tier-save")
      // TODO: add an assert that proves that the new rate limit tier is shown and selected
    }

  }

}
