package support

import utils.WireMockExtensions
import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.ApmConnector
import model.CombinedApi
import play.api.http.Status

trait ApmServiceStub extends WireMockExtensions {
  def primeFetchAllCombinedApisSuccess(combinedApis: List[CombinedApi]): Unit = {
   
    stubFor(get(urlEqualTo("/combined-rest-xml-apis"))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withJsonBody(combinedApis)
      )
    )
  }

 


}