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

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithSubscriptionFields

trait ApplicationWithSubscriptionDataTestData extends CommonTestData with SubscriptionsTestData with ApplicationTestData {
  val applicationWithSubscriptionData = ApplicationWithSubscriptionFields(defaultApplication.details, defaultApplication.collaborators, defaultSubscriptions, Map.empty)

  val blockedApplicationWithSubscriptionData = ApplicationWithSubscriptionFields(blockedApplication.details, blockedApplication.collaborators, defaultSubscriptions, Map.empty)

  val pendingApprovalApplicationWithSubscriptionData =
    ApplicationWithSubscriptionFields(pendingApprovalApplication.details, pendingApprovalApplication.collaborators, defaultSubscriptions, Map.empty)

  implicit class ApplicationWithSubscriptionDataExtension(applicationWithSubscriptionData: ApplicationWithSubscriptionFields) {

    def toJson       = Json.toJson(applicationWithSubscriptionData)
    def toJsonString = Json.toJson(applicationWithSubscriptionData).toString
  }
}
