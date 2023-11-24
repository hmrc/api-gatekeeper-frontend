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

package uk.gov.hmrc.gatekeeper.controllers

import java.time.{LocalDateTime, Period}
import scala.concurrent.Future

import mocks.connectors._
import mocks.services._
import org.mockito.stubbing.ScalaOngoingStubbing
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.test.FakeRequest

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.Standard
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, ClientId}
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationServiceMockModule, StrideAuthorisationServiceMockModule}
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker

trait ControllerSetupBase
    extends MockitoSugar
    with ArgumentMatchersSugar
    with StrideAuthorisationServiceMockModule
    with LdapAuthorisationServiceMockModule
    with ApiDefinitionServiceMockProvider
    with DeveloperServiceMockProvider
    with ApplicationServiceMockProvider
    with ApiCataloguePublishConnectorMockProvider
    with ApmServiceMockProvider
    with DeploymentApprovalServiceMockProvider
    with CollaboratorTracker
    with CommandConnectorMockProvider {

  val mockDeveloperConnector = mock[DeveloperConnector]
  val grantLength: Period    = Period.ofDays(547)

  val basicApplication = ApplicationResponse(
    ApplicationId.random,
    ClientId.random,
    "gatewayId1",
    "application1",
    "PRODUCTION",
    None,
    Set("sample@example.com".toLaxEmail.asAdministratorCollaborator, "someone@example.com".toLaxEmail.asDeveloperCollaborator),
    LocalDateTime.now(),
    Some(LocalDateTime.now()),
    Standard(),
    ApplicationState(),
    grantLength
  )
  val application      = ApplicationWithHistory(basicApplication, List.empty)
  val applicationId    = application.application.id

  val authToken                 = GatekeeperSessionKeys.AuthToken    -> "some-bearer-token"
  val userToken                 = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val superUserToken            = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val adminToken                = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val aLoggedInRequest          = FakeRequest().withSession(authToken, userToken)
  val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
  val anAdminLoggedInRequest    = FakeRequest().withSession(authToken, adminToken)
  val aLoggedOutRequest         = FakeRequest().withSession()
  val noDevs                    = List.empty[Developer]

  def givenTheAppWillBeReturned(): ScalaOngoingStubbing[Future[ApplicationWithHistory]] = ApplicationServiceMock.FetchApplication.returns(application)
}
