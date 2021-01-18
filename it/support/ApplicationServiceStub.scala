package support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.libs.json.Json
import model.NewModel

trait ApplicationServiceStub {
  val collaboratorsUrl = "/collaborators?context=api1&version=1"

  def primeApplicationServiceSuccessWithUsers(users: Seq[NewModel.RegisteredUser]): Unit = {

    stubFor(get(urlEqualTo(collaboratorsUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(users.map(_.email)).toString())))
  }

 


}
