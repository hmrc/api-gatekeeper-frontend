package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.connectors.ApplicationConnector
import uk.gov.hmrc.gatekeeper.models.ApiContext
import uk.gov.hmrc.gatekeeper.models.ApiVersion
import uk.gov.hmrc.gatekeeper.utils.WireMockExtensions

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
