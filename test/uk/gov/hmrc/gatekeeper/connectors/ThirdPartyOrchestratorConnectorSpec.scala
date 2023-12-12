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

import java.time.LocalDateTime
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatestplus.play.guice.GuiceOneAppPerSuite

import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Access
import uk.gov.hmrc.apiplatform.modules.applications.common.domain.models.FullName
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationResponse, _}
import uk.gov.hmrc.apiplatform.modules.applications.submissions.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.utils._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ThirdPartyOrchestratorConnectorSpec
    extends AsyncHmrcSpec
    with WireMockSugar
    with GuiceOneAppPerSuite
    with UrlEncoding
    with FixedClock {

  trait Setup {
    implicit val hc: HeaderCarrier = HeaderCarrier()

    val httpClient = app.injector.instanceOf[HttpClient]

    val mockConnectorConfig: ThirdPartyOrchestratorConnector.Config = mock[ThirdPartyOrchestratorConnector.Config]
    when(mockConnectorConfig.serviceBaseUrl).thenReturn(wireMockUrl)

    val applicationId = ApplicationId.random

    val application = buildApplication(applicationId, ClientId.random, UserId.random)

    val underTest = new ThirdPartyOrchestratorConnector(httpClient, mockConnectorConfig)
  }

  "getApplication" should {
    "return application" in new Setup {
      val url     = s"/applications/${applicationId}"
      val payload = Json.toJson(application)

      stubFor(
        get(urlEqualTo(url))
          .willReturn(
            aResponse()
              .withStatus(OK)
              .withBody(payload.toString)
          )
      )

      val result = await(underTest.getApplication(applicationId))
      result should not be None

      result.map { app =>
        app.id shouldBe application.id
      }
    }
  }

  private def buildApplication(applicationId: ApplicationId, clientId: ClientId, userId1: UserId): ApplicationResponse = {
    val standardAccess = Access.Standard(
      importantSubmissionData = Some(ImportantSubmissionData(
        organisationUrl = Some("https://www.example.com"),
        responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
        serverLocations = Set(ServerLocation.InUK),
        termsAndConditionsLocation = TermsAndConditionsLocations.InDesktopSoftware,
        privacyPolicyLocation = PrivacyPolicyLocations.InDesktopSoftware,
        termsOfUseAcceptances = List(TermsOfUseAcceptance(
          responsibleIndividual = ResponsibleIndividual(FullName("Bob Fleming"), LaxEmailAddress("bob@example.com")),
          dateTime = LocalDateTime.parse("2022-10-08T12:24:31.123"),
          submissionId = SubmissionId.random,
          submissionInstance = 0
        ))
      ))
    )

    def buildCollaborator(userId: UserId) = {
      Collaborator(
        emailAddress = LaxEmailAddress("bob@example.com"),
        role = Collaborator.Roles.ADMINISTRATOR,
        userId = userId
      )
    }

    ApplicationResponse(
      id = applicationId,
      clientId = clientId,
      gatewayId = "gateway-id",
      name = "Petes test application",
      deployedTo = Environment.PRODUCTION,
      description = Some("Petes test application description"),
      collaborators = Set(buildCollaborator(userId1)),
      createdOn = LocalDateTime.parse("2022-12-23T12:24:31.123"),
      lastAccess = Some(LocalDateTime.parse("2023-10-02T12:24:31.123")),
      grantLength = 18,
      lastAccessTokenUsage = None,
      termsAndConditionsUrl = None,
      privacyPolicyUrl = None,
      access = standardAccess,
      state = ApplicationState(name = State.TESTING, updatedOn = LocalDateTime.parse("2022-10-08T12:24:31.123")),
      rateLimitTier = RateLimitTier.BRONZE,
      checkInformation = None,
      blocked = false,
      trusted = false,
      ipAllowlist = IpAllowlist(false, Set.empty),
      moreApplication = MoreApplication(false)
    )
  }
}
