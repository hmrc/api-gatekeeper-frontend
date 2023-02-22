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

package uk.gov.hmrc.gatekeeper.controllers.apicataloguepublish

import uk.gov.hmrc.gatekeeper.support.ServerBaseISpec
import uk.gov.hmrc.gatekeeper.support.AuthServiceStub
import uk.gov.hmrc.gatekeeper.utils.{MockCookies, UserFunctionsWrapper}
import org.scalatest.{BeforeAndAfterEach, Suite}
import org.scalatestplus.play.ServerProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSResponse
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.test.Helpers.{FORBIDDEN, OK}
import org.jsoup.Jsoup
import uk.gov.hmrc.gatekeeper.support.ApiCataloguePublishStub
import play.filters.csrf.CSRF
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector

class ApiCataloguePublishControllerISpec extends ServerBaseISpec with BeforeAndAfterEach with UserFunctionsWrapper
    with AuthServiceStub with ApiCataloguePublishStub {
  this: Suite with ServerProvider =>

  protected override def appBuilder: GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .configure(
        "microservice.services.auth.host"                               -> wireMockHost,
        "microservice.services.auth.port"                               -> wireMockPort,
        "metrics.enabled"                                               -> true,
        "auditing.enabled"                                              -> false,
        "auditing.consumer.baseUri.host"                                -> wireMockHost,
        "auditing.consumer.baseUri.port"                                -> wireMockPort,
        "microservice.services.api-platform-api-catalogue-publish.host" -> wireMockHost,
        "microservice.services.api-platform-api-catalogue-publish.port" -> wireMockPort
      )

  val url = s"http://localhost:$port"

  val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val tokenProvider      = app.injector.instanceOf[CSRF.TokenProviderProvider]
  val validHeaders       = List(CONTENT_TYPE -> "application/x-www-form-urlencoded", "csrfToken" -> tokenProvider.get.generateToken)

  def callGetEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withCookies(MockCookies.makeWsCookie(app))
      .withFollowRedirects(false)
      .get()
      .futureValue

  def callPostEndpoint(url: String, headers: List[(String, String)]): WSResponse =
    wsClient
      .url(url)
      .withHttpHeaders(headers: _*)
      .withCookies(MockCookies.makeWsCookie(app))
      .withFollowRedirects(false)
      .post("")
      .futureValue

  "ApiCataloguePublishController" when {

    "GET /api-gatekeeper/apicataloguepublish/start" should {
      "respond with 200 and render start correctly when authorised" in {
        primeAuthServiceSuccess()
        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/start", validHeaders)
        result.status shouldBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("heading").text() shouldBe "Publish Page"

      }

      "respond with 200 and render forbidden page when unauthorised" in {
        primeAuthServiceFail()
        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/start", validHeaders)
        result.status shouldBe FORBIDDEN
      }
    }

    "GET /api-gatekeeper/apicataloguepublish/publish" should {
      "respond with 200 and render publish  correctly when authorised" in {
        primeAuthServiceSuccess()
        primePublishByServiceName(OK, "myservice", ApiCataloguePublishConnector.PublishResponse("id", "publishref", "API_PLATFORM"))

        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/publish?serviceName=myservice", validHeaders)
        result.status shouldBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("heading").text() shouldBe "Publish Page"
      }

      "respond with 403 when not authorised" in {
        primeAuthServiceFail()

        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/publish?serviceName=myservice", validHeaders)
        result.status shouldBe FORBIDDEN
      }

    }

    "GET /api-gatekeeper/apicataloguepublish/publishall" should {
      "respond with 200 and render publish  correctly when authorised" in {
        primeAuthServiceSuccess()
        primePublishAll(OK)

        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/publishall", validHeaders)
        result.status shouldBe OK

        val document = Jsoup.parse(result.body)
        document.getElementById("heading").text() shouldBe "Publish Page"
      }

      "respond with 403 and render publish  correctly when authorised" in {
        primeAuthServiceFail()

        val result = callGetEndpoint(s"$url/api-gatekeeper/apicatalogue/publishall", validHeaders)
        result.status shouldBe FORBIDDEN
      }

    }
  }
}
