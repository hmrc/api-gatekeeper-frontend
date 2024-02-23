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

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.events.connectors.DisplayEvent
import uk.gov.hmrc.gatekeeper.common.{BaseSpec, SignInSugar, WebPage}
import uk.gov.hmrc.gatekeeper.matchers.CustomMatchers
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.stubs._
import uk.gov.hmrc.gatekeeper.testdata.{AllSubscribeableApisTestData, ApiDefinitionTestData}
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiGatekeeperBaseSpec
    extends BaseSpec
    with SignInSugar
    with Matchers
    with CustomMatchers
    with GivenWhenThen
    with AllSubscribeableApisTestData
    with ApiDefinitionTestData
    with UrlEncoding
    with ThirdPartyApplicationStub
    with ThirdPartyDeveloperStub
    with ApiPlatformMicroserviceStub {

  def stubApplication(application: String, developers: List[RegisteredUser], stateHistory: String, appId: ApplicationId, events: List[DisplayEvent] = Nil) = {
    stubNewApplication(application, appId)
    stubStateHistory(stateHistory, appId)
    stubApiDefintionsForApplication(allSubscribeableApis, appId)
    stubDevelopers(developers)

    stubGetDeveloper(developers.head.email, Json.stringify(Json.toJson(developers.head)))
    stubSubmissionLatestIsNotFound(appId)
  }

  def isCurrentPage(page: WebPage): Unit = {
    page.heading shouldBe page.pageHeading
  }
}
