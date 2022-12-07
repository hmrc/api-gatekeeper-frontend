package uk.gov.hmrc.gatekeeper.support

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status

trait AuditServiceStub {
  val auditUrl                      = "/write/audit"
  val auditMergedUrl                = "/write/audit/merged"
  private val auditUrlMAtcher       = urlEqualTo(auditUrl)
  private val auditMergedUrlMAtcher = urlEqualTo(auditMergedUrl)

  def primeAuditService() = {
    stubFor(post(auditUrlMAtcher)
      .willReturn(
        aResponse()
          .withStatus(Status.NO_CONTENT)
      ))

    stubFor(post(auditMergedUrlMAtcher)
      .willReturn(
        aResponse()
          .withStatus(Status.NO_CONTENT)
      ))
  }
}
