package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import model.User

trait DeveloperService {
  val emailPreferencesUrl = "/developers/email-preferences"
  val allUrl = "/developers/all"
  val byEmails = "/developers/get-by-emails"

  def primeDeveloperServiceAllSuccessWithUsers(users: Seq[User]): Unit = {

    stubFor(get(urlEqualTo(allUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }

  def primeDeveloperServiceGetByEmails(users: Seq[User]): Unit = {

    stubFor(post(urlEqualTo(byEmails))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }
 


}
