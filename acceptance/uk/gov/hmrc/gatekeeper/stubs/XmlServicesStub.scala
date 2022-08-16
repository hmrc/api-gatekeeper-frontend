package uk.gov.hmrc.gatekeeper.stubs

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, stubFor, urlEqualTo}
import play.api.http.Status.OK
import uk.gov.hmrc.gatekeeper.models.UserId
import uk.gov.hmrc.gatekeeper.specs.MockDataSugar.xmlApis

trait XmlServicesStub {


  def stubGetAllXmlApis(): Unit = {
    stubFor(get(urlEqualTo("/api-platform-xml-services/xml/apis"))
      .willReturn(aResponse().withBody(xmlApis).withStatus(OK)))
  }

  def stubGetXmlOrganisationsForUser(userId: UserId): Unit = {
    stubFor(get(urlEqualTo(s"/api-platform-xml-services/organisations?userId=${userId.value}&sortBy=ORGANISATION_NAME"))
      .willReturn(aResponse().withBody("[]").withStatus(OK)))
  }

  def stubGetXmlApiForCategories(): Unit = {
    stubFor(get(urlEqualTo("/api-platform-xml-services/xml/apis/filtered"))
      .willReturn(aResponse().withBody(xmlApis).withStatus(OK)))
  }


}
