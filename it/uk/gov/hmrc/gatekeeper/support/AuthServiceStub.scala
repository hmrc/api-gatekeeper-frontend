package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status

trait AuthServiceStub {
  val authUrl                = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)

  val headersWithBearer = Map()

  def primeAuthServiceNoClientId(body: String): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(body))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |}""".stripMargin
          )
      ))
  }

  def primeAuthServiceSuccess(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [ { "key" : "user-role", "identifiers": [], "state": "123"} ],
               |  "optionalName": {"name": "bob", "lastName": "hope"}
               |}""".stripMargin
          )
      ))
  }

  def primeAuthServiceFail(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
      ))
  }
}
