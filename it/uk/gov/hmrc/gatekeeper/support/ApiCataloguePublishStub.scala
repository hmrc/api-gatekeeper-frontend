package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector

trait ApiCataloguePublishStub {
  val publishByServiceNameUrl = "/api-platform-api-catalogue-publish/publish/"
  val publishAllUrl           = "/api-platform-api-catalogue-publish/publish-all"

  def primePublishByServiceName(status: Int, serviceName: String, responseBody: ApiCataloguePublishConnector.PublishResponse): Unit = {
    stubFor(post(urlEqualTo(s"$publishByServiceNameUrl$serviceName"))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(responseBody).toString())
      ))
  }

  def primePublishAll(status: Int): Unit = {
    stubFor(post(urlEqualTo(publishAllUrl))
      .willReturn(
        aResponse()
          .withStatus(status)
          .withBody(Json.toJson(ApiCataloguePublishConnector.PublishAllResponse("Happy Happy")).toString())
      ))
  }
}
