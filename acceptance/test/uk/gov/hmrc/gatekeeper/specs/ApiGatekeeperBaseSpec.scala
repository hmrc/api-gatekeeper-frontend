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

package uk.gov.hmrc.gatekeeper.specs

import org.scalatest.GivenWhenThen
import org.scalatest.matchers.should.Matchers

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithSubscriptionFields, CollaboratorData, StateHistory}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.common.utils.WireMockExtensions
import uk.gov.hmrc.apiplatform.modules.events.connectors.DisplayEvent
import uk.gov.hmrc.gatekeeper.common.{BaseSpec, SignInSugar, WebPage}
import uk.gov.hmrc.gatekeeper.matchers.CustomMatchers
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.stubs._
import uk.gov.hmrc.gatekeeper.testdata.{AllSubscribeableApisTestData, ApiDefinitionTestData, ApplicationResponseTestData, StateHistoryTestData}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiGatekeeperBaseSpec
    extends BaseSpec
    with SignInSugar
    with Matchers
    with CustomMatchers
    with GivenWhenThen
    with AllSubscribeableApisTestData
    with ApiDefinitionTestData
    with StateHistoryTestData
    with ApplicationResponseTestData
    with UrlEncoding
    with WireMockExtensions
    with ThirdPartyApplicationStub
    with TpoApplicationQueryStub
    with ThirdPartyDeveloperStub
    with ApiPlatformMicroserviceStub {

  def stubApplication(
      appWithSubsFields: ApplicationWithSubscriptionFields,
      developers: List[RegisteredUser],
      stateHistory: List[StateHistory],
      appId: ApplicationId,
      events: List[DisplayEvent] = Nil
    ) = {
    stubAppQueryForActionBuilders(appId, appWithSubsFields.modify(_.withId(appId)), stateHistory)
    // Now also mock new single route when we're only after AppWithCollaborator and State History
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)
    stubHasTermsOfUseInvitation(appId, false)
    stubGetDeveloper(developers.head.email, Json.stringify(Json.toJson(developers.head)))
    stubSubmissionLatestIsNotFound(appId)
  }

  def stubApplicationWithVerifiedAdmin(
      appWithSubsFields: ApplicationWithSubscriptionFields,
      developers: List[RegisteredUser],
      stateHistory: List[StateHistory],
      appId: ApplicationId,
      events: List[DisplayEvent] = Nil
    ) = {

    stubAppQueryForActionBuilders(
      appId,
      appWithSubsFields.copy(collaborators =
        appWithSubsFields.collaborators ++ Set(CollaboratorData.Administrator.one.copy(emailAddress = LaxEmailAddress("dixie.fakename@example.com")))
      ),
      stateHistory
    )
    // Now also mock new single route when we're only after AppWithCollaborator and State History
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)
    stubHasTermsOfUseInvitation(appId, false)
    stubGetDeveloper(developers.head.email, Json.stringify(Json.toJson(developers.head)))
    stubSubmissionLatestIsNotFound(appId)
  }

  def isCurrentPage(page: WebPage): Unit = {
    page.heading shouldBe page.pageHeading
  }
}
