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

package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.models.{ClientId, RateLimitTier}
import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.models.ApplicationResponse

trait ApplicationResponseTestData extends ApplicationResponseBuilder with CollaboratorsTestData with AccessTestData with ApplicationStateTestData {

  val defaultApplicationResponse = DefaultApplicationResponse
    .withId(applicationId)
    .withName(applicationName)
    .withDescription("application for test")
    .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
    .withGatewayId("12345")
    .deployedToProduction
    .withCollaborators(collaboratorsDevAndUnverifiedAdmin)
    .withState(stateForFetchAppResponseByEmail)
    .withAccess(standardAccess)
    .unblocked
    .withRateLimitTier(RateLimitTier.BRONZE)
    .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
    .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

  val blockedApplicationResponse = defaultApplicationResponse
    .withId(blockedApplicationId)
    .withName(blockedApplicationName)
    .withBlocked(true)

  val pendingApprovalApplicationResponse = defaultApplicationResponse
    .withId(pendingApprovalApplicationId)
    .withName(pendingApprovalApplicationName)
    .withState(pendingApprovalState)

  implicit class ApplicationResponseSeqExtension(applicationResponses: Seq[ApplicationResponse]) {
    def toJson       = Json.toJson(applicationResponses)
    def toJsonString = Json.toJson(applicationResponses).toString
  }
}
