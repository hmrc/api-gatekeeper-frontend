package support

import uk.gov.hmrc.gatekeeper.utils.WireMockExtensions
import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.gatekeeper.models.CombinedApi
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