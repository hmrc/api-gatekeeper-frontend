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

package uk.gov.hmrc.gatekeeper.testdata

import java.time.Instant

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models.AccessFixtures
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, RateLimitTier}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder

trait ApplicationResponseTestData extends ApplicationBuilder with CollaboratorsTestData with AccessFixtures with ApplicationStateTestData {

  val defaultApplicationResponse = DefaultApplication
    .withId(applicationId)
    .withName(applicationName)
    .withDescription("application for test")
    .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
    .withGatewayId("12345")
    .deployedToProduction
    .withCollaborators(collaboratorsDevAndUnverifiedAdmin)
    .withState(stateForFetchAppResponseByEmail)
    .withAccess(standardAccessOne)
    .unblocked
    .withRateLimitTier(RateLimitTier.BRONZE)
    .withCreatedOn(Instant.parse("2016-04-08T10:24:40.651Z"))
    .withLastAccess(Instant.parse("2019-07-01T00:00:00.000Z"))

  val blockedApplicationResponse = defaultApplicationResponse
    .withId(blockedApplicationId)
    .withName(blockedApplicationName)
    .withBlocked(true)

  val pendingApprovalApplicationResponse = defaultApplicationResponse
    .withId(pendingApprovalApplicationId)
    .withName(pendingApprovalApplicationName)
    .withState(pendingApprovalState)

  implicit class ApplicationResponseExtension(applicationResponse: ApplicationWithCollaborators) {
    def toJson       = Json.toJson(applicationResponse)
    def toJsonString = Json.toJson(applicationResponse).toString
  }

  implicit class ApplicationResponseSeqExtension(applicationResponses: Seq[ApplicationWithCollaborators]) {
    def toJson       = Json.toJson(applicationResponses)
    def toJsonString = Json.toJson(applicationResponses).toString
  }
}
