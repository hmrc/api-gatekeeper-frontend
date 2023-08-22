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

package uk.gov.hmrc.gatekeeper.common

import uk.gov.hmrc.gatekeeper.matchers.CustomMatchers
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar
import com.github.tomakehurst.wiremock.client.WireMock._
import org.openqa.selenium.{By, NoSuchElementException}
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector.FindUserIdRequest
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector.FindUserIdResponse
import play.api.http.Status._
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.RateLimitTier

trait ApprovedBaseSpec extends BaseSpec
  with SignInSugar with Matchers with CustomMatchers with UrlEncoding {

  import MockDataSugar._

  implicit val driver = webDriver

  protected def stubRateLimitTier(applicationId: String, tier: String) = {
    stubFor(post(urlEqualTo(s"/application/$applicationId/rate-limit-tier"))
      .withRequestBody(equalTo(s"""{"rateLimitTier":"$tier"}""".stripMargin))
      .willReturn(aResponse().withStatus(NO_CONTENT)))
  }

  protected def stubGetDeveloper(email: String, userJsonText: String, userId: UserId = UserId.random) = {
    val requestJson = Json.stringify(Json.toJson(FindUserIdRequest(email.toLaxEmail)))
    implicit val format = Json.writes[FindUserIdResponse]
    val responseJson = Json.stringify(Json.toJson(FindUserIdResponse(userId)))

    stubFor(post(urlEqualTo("/developers/find-user-id"))
      .withRequestBody(equalToJson(requestJson))
      .willReturn(aResponse().withStatus(OK).withBody(responseJson)))

    stubFor(
      get(urlPathEqualTo("/developer"))
      .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
      .willReturn(
        aResponse().withStatus(OK).withBody(userJsonText)
      )
    )
  }

  protected def stubApplicationListAndDevelopers() = {
    val expectedAdmins = s"""[${administrator()},${administrator(admin2Email, "Admin", "McAdmin")}]""".stripMargin

    stubFor(get(urlEqualTo("/gatekeeper/applications"))
      .willReturn(aResponse().withBody(approvedApplications).withStatus(OK)))

    stubGetDeveloper(adminEmail,administrator())

    stubFor(post(urlEqualTo(s"/developers"))
      .willReturn(aResponse().withBody(expectedAdmins).withStatus(OK)))
  }

  protected def assertApplicationRateLimitTier(isSuperUser: Boolean, rateLimitTier: String) = {
    if (isSuperUser) {
      id("rate-limit-tier").element.text shouldBe s"Rate limit tier: $rateLimitTier"
      id("rate-limit-tier-table").element.text should containInOrder(List("BRONZE", "SILVER", "GOLD", "PLATINUM", "RHODIUM", "Save new rate limit tier"))
      id(rateLimitTier).element.isSelected shouldBe true
    } else {
      intercept[NoSuchElementException] {
        webDriver.findElement(By.id("rate-limit-tier"))
      }
      intercept[NoSuchElementException] {
        webDriver.findElement(By.id("rate-limit-tier-table"))
      }
    }
  }

  protected def assertApplicationDetails(isSuperUser: Boolean = false) = {
    verifyText("data-submitter-name", s"$firstName $lastName")
    verifyText("data-submitter-email", adminEmail)
    assertApplicationRateLimitTier(isSuperUser, rateLimitTier = RateLimitTier.BRONZE.toString)
    id("admins").element.text should containInOrder(List(s"$firstName $lastName", adminEmail, "Admin McAdmin", admin2Email))
    verifyText("data-submitted-on", "Submitted: 22 March 2016")
    verifyText("data-approved-on", "Approved: 05 April 2016")
    verifyText("data-approved-by", "Approved by: gatekeeper.username")
  }

}
