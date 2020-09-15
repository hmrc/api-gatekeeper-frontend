package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import uk.gov.hmrc.auth.core.authorise.Predicate

trait AuthServiceStub {
  val authUrl = "/auth/authorise"
  private val authUrlMatcher = urlEqualTo(authUrl)


  def primeAuthServiceNoClientId(body: String): Unit = {
    stubFor(post(authUrlMatcher)
      .withRequestBody(equalToJson(body))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |}""".stripMargin)
      )
    )
  }

  def primeAuthServiceSuccess(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(
            s"""{
               |  "authorisedEnrolments": [ ],
               |  "name": {"name": "bob", "lastName": "hope"}
               |}""".stripMargin)
      )
    )
  }

  def primeAuthServiceFail(): Unit = {
    stubFor(post(authUrlMatcher)
      .willReturn(
        aResponse()
          .withStatus(Status.UNAUTHORIZED)
          .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")

      )
    )
  }


}
