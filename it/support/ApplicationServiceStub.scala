package support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import model.RegisteredUser
import connectors.ApplicationConnector
import model.ApiContext
import model.ApiVersion
import utils.WireMockExtensions

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
