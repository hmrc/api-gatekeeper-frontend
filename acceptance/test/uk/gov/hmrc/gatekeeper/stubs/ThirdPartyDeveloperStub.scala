/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._
import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.testdata.{CommonTestData, MockDataSugar}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

trait ThirdPartyDeveloperStub extends WireMockExtensions with CommonTestData with UrlEncoding {
  import MockDataSugar._

  def stubDeveloper(user: RegisteredUser) = {
    stubFor(
      get(urlPathEqualTo("/developer"))
        .withQueryParam("developerId", equalTo(user.userId.value.toString))
        .willReturn(
          aResponse().withStatus(OK).withBody(unverifiedUserJson)
        )
    )
  }

  protected def stubGetDeveloper(email: LaxEmailAddress, userJsonText: String, userId: UserId = UserId.random) = {
    implicit val format = Json.writes[DeveloperConnector.FindUserIdResponse]
    val responseJson    = Json.stringify(Json.toJson(DeveloperConnector.FindUserIdResponse(userId)))

    stubFor(post(urlEqualTo("/developers/find-user-id"))
      .willReturn(aResponse().withStatus(OK).withBody(responseJson)))

    stubFor(
      get(urlPathEqualTo("/developer"))
        .withQueryParam("developerId", equalTo(encode(userId.value.toString)))
        .willReturn(
          aResponse().withStatus(OK).withBody(userJsonText)
        )
    )

  }

  def stubGetDevelopersByEmails(developers: Seq[RegisteredUser]): Unit = {
    val emailsResponseJson = Json.toJson(developers).toString()

    stubFor(
      post(urlPathEqualTo("/developers/get-by-emails"))
        .willReturn(aResponse()
          .withBody(emailsResponseJson)
          .withStatus(OK))
    )
  }

  def stubDevelopersSearch(emailFilter: String, developers: Seq[RegisteredUser]): Unit = {
    val developersListJson: String = Json.toJson(developers).toString

    val body = java.net.URLEncoder.encode("emailFilter=" + emailFilter, "UTF-8")
    stubFor(
      get(urlPathEqualTo("/developers"))
        .withRequestBody(equalTo(body))
        .willReturn(aResponse()
          .withBody(developersListJson)
          .withStatus(OK))
    )
  }

  def stubRandomDevelopers(randomDevelopersCount: Int): Unit = {
    val developersList: String = developerListJsonGenerator(randomDevelopersCount).get
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse()
        .withBody(developersList)
        .withStatus(OK)))
  }

  def stubDevelopers(): Unit = {
    stubFor(get(urlEqualTo("/developers/all"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubDevelopers(developers: List[RegisteredUser]) = {
    stubFor(get(urlMatching(s"/developers")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
    stubFor(post(urlMatching(s"/developers/get-by-emails")).willReturn(aResponse().withBody(Json.toJson(developers).toString())))
  }

  def stubDevelopersSearch(): Unit = {
    stubFor(post(urlEqualTo("/developers/search"))
      .willReturn(aResponse().withBody(allUsers).withStatus(OK)))
  }

  def stubDeveloper(): Unit = {

    implicit val format = Json.writes[DeveloperConnector.FindUserIdResponse]

    stubFor(
      post(urlEqualTo("/developers/find-user-id"))
        .withJsonRequestBody(DeveloperConnector.FindUserIdRequest(developer8.toLaxEmail))
        .willReturn(
          aResponse()
            .withStatus(OK)
            .withJsonBody(DeveloperConnector.FindUserIdResponse(UserId(developer8Id)))
        )
    )

    stubFor(
      get(urlPathEqualTo("/developer"))
        .withQueryParam("developerId", equalTo(encode(developer8Id.toString)))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withStatus(OK)
            .withBody(user)
        )
    )
  }

  def stubRemoveMfa(): Unit = {
    stubFor(post(urlEqualTo(s"/developer/${developer8Id}/mfa/remove"))
      .willReturn(aResponse().withStatus(OK).withBody(userWithoutMfaDetails)))
  }

  def stubDeveloperGetUnverified() = {
    stubFor(
      get(urlPathEqualTo("/developer"))
        .willReturn(
          aResponse().withStatus(OK).withBody(unverifiedUserJson)
        )
    )
  }

}
