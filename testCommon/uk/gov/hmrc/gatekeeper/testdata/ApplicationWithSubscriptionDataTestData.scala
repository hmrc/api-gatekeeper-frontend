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

import uk.gov.hmrc.gatekeeper.models.applications.ApplicationWithSubscriptionData
import play.api.libs.json.Json

trait ApplicationWithSubscriptionDataTestData extends CommonTestData with SubscriptionsTestData with ApplicationTestData {
  val applicationWithSubscriptionData = ApplicationWithSubscriptionData(defaultApplication, defaultSubscriptions, Map.empty)

  val blockedApplicationWithSubscriptionData = ApplicationWithSubscriptionData(blockedApplication, defaultSubscriptions, Map.empty)

  val pendingApprovalApplicationWithSubscriptionData = ApplicationWithSubscriptionData(pendingApprovalApplication, defaultSubscriptions, Map.empty)

  implicit class ApplicationWithSubscriptionDataExtension(applicationWithSubscriptionData: ApplicationWithSubscriptionData) {
    import uk.gov.hmrc.gatekeeper.models.APIDefinitionFormatters._
    implicit val ApplicationWithSubscriptionDataFormat = Json.format[ApplicationWithSubscriptionData]

    def toJson       = Json.toJson(applicationWithSubscriptionData)
    def toJsonString = Json.toJson(applicationWithSubscriptionData).toString
  }
}
