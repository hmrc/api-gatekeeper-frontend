package support

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ApplicationConnector
import model.{ApiContext, ApiVersion, RegisteredUser}
import utils.WireMockExtensions

import play.api.http.Status

trait ApplicationServiceStub extends WireMockExtensions {
  def primeApplicationServiceSuccessWithUsers(users: Seq[RegisteredUser]): Unit = {
    val request = ApplicationConnector.SearchCollaboratorsRequest(ApiContext("api1"), ApiVersion("1"), None)
   
    stubFor(post(urlEqualTo("/collaborators"))
      .withJsonRequestBody(request)
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withJsonBody(users.map(_.email))
      )
    )
  }

 


}
