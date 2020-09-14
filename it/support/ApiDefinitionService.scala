package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import model.APIDefinition

trait ApiDefinitionService {
  val apiPublicDefinitionUrl = "/api-definition"
  val apiPrivateDefinitionUrl = "/api-definition?type=private"


  def primeDefinitionServiceSuccessWithPublicApis(apis: Seq[APIDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPublicDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }

    def primeDefinitionServiceSuccessWithPrivateApis(apis: Seq[APIDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPrivateDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }
 


}
