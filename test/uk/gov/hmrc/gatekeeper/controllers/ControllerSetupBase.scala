/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import play.api.test.FakeRequest
import mocks.services._

import scala.concurrent.Future
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker
import mocks.connectors._
import org.mockito.stubbing.ScalaOngoingStubbing
import uk.gov.hmrc.gatekeeper.connectors.DeveloperConnector
import uk.gov.hmrc.gatekeeper.models._
import org.joda.time.DateTime
import uk.gov.hmrc.modules.gkauth.services.StrideAuthorisationServiceMockModule
import java.time.Period

trait ControllerSetupBase
    extends MockitoSugar
    with ArgumentMatchersSugar
    with StrideAuthorisationServiceMockModule
    with ApiDefinitionServiceMockProvider
    with DeveloperServiceMockProvider
    with ApplicationServiceMockProvider
    with ApiCataloguePublishConnectorMockProvider
    with ApmServiceMockProvider
    with DeploymentApprovalServiceMockProvider
    with CollaboratorTracker {

  val mockDeveloperConnector = mock[DeveloperConnector]
  val grantLength: Period = Period.ofDays(547)

  val basicApplication = ApplicationResponse(
    ApplicationId.random,
    ClientId.random,
    "gatewayId1",
    "application1",
    "PRODUCTION",
    None,
    Set("sample@example.com".asAdministratorCollaborator, "someone@example.com".asDeveloperCollaborator),
    DateTime.now(),
    Some(DateTime.now()),
    Standard(),
    ApplicationState(),
    grantLength)
  val application = ApplicationWithHistory(basicApplication, List.empty)
  val applicationId = application.application.id

  val authToken = GatekeeperSessionKeys.AuthToken -> "some-bearer-token"
  val userToken = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val superUserToken = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val adminToken = GatekeeperSessionKeys.LoggedInUser -> "Bobby Example"
  val aLoggedInRequest = FakeRequest().withSession(authToken, userToken)
  val aSuperUserLoggedInRequest = FakeRequest().withSession(authToken, superUserToken)
  val anAdminLoggedInRequest = FakeRequest().withSession(authToken, adminToken)
  val aLoggedOutRequest = FakeRequest().withSession()
  val noDevs = List.empty[Developer]

  def givenTheAppWillBeReturned(): ScalaOngoingStubbing[Future[ApplicationWithHistory]] = ApplicationServiceMock.FetchApplication.returns(application)
}
