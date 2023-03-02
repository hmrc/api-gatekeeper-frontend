/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gatekeeper.connectors

import java.time.{LocalDateTime, Period}
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.joda.time.DateTime
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators.Administrator
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{ApplicationId, Collaborators}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{DispatchRequest, DispatchSuccessResult, RemoveCollaborator}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.Actors
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class CommandConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding {

  def anApplicationResponse(createdOn: DateTime = DateTime.now(), lastAccess: DateTime = DateTime.now()): ApplicationResponse = {
    ApplicationResponse(
      ApplicationId.random,
      ClientId("clientid"),
      "gatewayId",
      "appName",
      "deployedTo",
      None,
      Set.empty,
      createdOn,
      Some(lastAccess),
      Privileged(),
      ApplicationState(),
      Period.ofDays(547),
      RateLimitTier.BRONZE,
      Some("termsUrl"),
      Some("privacyPolicyUrl"),
      None
    )
  }

  val apiVersion1   = ApiVersion.random
  val applicationId = ApplicationId.random
  val administrator = Administrator(UserId.random, "sample@example.com".toLaxEmail)
  val developer     = Collaborators.Developer(UserId.random, "someone@example.com".toLaxEmail)

  val authToken   = "Bearer Token"
  implicit val hc = HeaderCarrier().withExtraHeaders(("Authorization", authToken))

  class Setup(proxyEnabled: Boolean = false) {

    val httpClient               = app.injector.instanceOf[HttpClient]
    val mockAppConfig: AppConfig = mock[AppConfig]
    when(mockAppConfig.applicationProductionBaseUrl).thenReturn(wireMockUrl)

    val connector = new ProductionCommandConnector(mockAppConfig, httpClient) {}

  }

  "dispatch" should {
    val emailAddressToRemove = "toRemove@example.com".toLaxEmail
    val gatekeeperUserName   = "maxpower"
    val collaborator         = Collaborators.Administrator(UserId.random, emailAddressToRemove)
    val command              = RemoveCollaborator(Actors.GatekeeperUser(gatekeeperUserName), collaborator, LocalDateTime.now())

    val adminsToEmail = Set("admin1@example.com", "admin2@example.com").map(_.toLaxEmail)
    val url           = s"/application/${applicationId.value.toString()}/dispatch"

    "send a correct command" in new Setup {
      stubFor(
        patch(urlPathEqualTo(url))
          .withJsonRequestBody(DispatchRequest(command, adminsToEmail))
          .willReturn(
            aResponse()
              .withJsonBody(DispatchSuccessResult(anApplicationResponse(), List.empty))
              .withStatus(OK)
          )
      )
      await(connector.dispatch(applicationId, command, adminsToEmail))
    }
  }
}