package support

import com.github.tomakehurst.wiremock.client.WireMock._
import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
import uk.gov.hmrc.gatekeeper.models.{APICategoryDetails, ApiDefinition}
import play.api.http.Status
import play.api.libs.json.Json

trait APIDefinitionServiceStub {
  val apiPublicDefinitionUrl = "/api-definition"
  val apiPrivateDefinitionUrl = "/api-definition?type=private"
  val getCategoriesUrl = "/api-categories"

  def primeDefinitionServiceSuccessWithPublicAPIs(apis: Seq[ApiDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPublicDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }

    def primeDefinitionServiceSuccessWithPrivateAPIs(apis: Seq[ApiDefinition]): Unit = {

    stubFor(get(urlEqualTo(apiPrivateDefinitionUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }

     def primeGetAllCategories(apis: Seq[APICategoryDetails]): Unit = {

    stubFor(get(urlEqualTo(getCategoriesUrl))
      .willReturn(
        aResponse()
          .withStatus(Status.OK)
          .withBody(Json.toJson(apis).toString())))
  }
 


}
