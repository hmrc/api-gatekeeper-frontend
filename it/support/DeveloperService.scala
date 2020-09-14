package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import model.User

trait DeveloperService {
  val emailPreferencesUrl = "/developers/email-preferences"
  val allUrl = "/developers/all"

  def primeDeveloperServiceAllSuccessWithUsers(users: Seq[User]): Unit = {

    stubFor(get(urlEqualTo(allUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users).toString())))
  }

 


}
