package support

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status
import play.api.libs.json.{JsArray, Json}
import model.APIDefinition
import model.APICategory

trait APIDefinitionServiceStub {
  val apiPublicDefinitionUrl = "/api-definition"
  val apiPrivateDefinitionUrl = "/api-definition?type=private"
  val getCategoriesUrl = "/api-categories"

  def primeDefinitionServiceSuccessWithPublicAPIs(apis: Seq[APIDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPublicDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }

    def primeDefinitionServiceSuccessWithPrivateAPIs(apis: Seq[APIDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPrivateDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }

     def primeGetAllCategories(apis: Seq[APICategory]): Unit = {

    stubFor(get(urlEqualTo(getCategoriesUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }
 


}
