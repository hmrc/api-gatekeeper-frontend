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
import java.util.UUID

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationName, ApplicationWithCollaboratorsFixtures}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, UserId}
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.domain.models._
import uk.gov.hmrc.gatekeeper.models._

trait CommonTestData extends ApplicationWithCollaboratorsFixtures {

  val applicationId                = standardApp.id
  val blockedApplicationId         = ApplicationId(UUID.fromString("fa38d130-7c8e-47d8-abc0-0374c7f73217"))
  val pendingApprovalApplicationId = ApplicationId(UUID.fromString("df0c32b6-bbb7-46eb-ba50-e6e5459162ff"))

  val applicationDescription         = standardApp.details.description.get
  val applicationName                = ApplicationName("My new app")
  val blockedApplicationName         = ApplicationName("Automated Test Application - Blocked")
  val pendingApprovalApplicationName = ApplicationName("Application requiring approval")

  val administratorEmail = "admin@example.com".toLaxEmail

  val developerEmail     = "purnima.fakename@example.com".toLaxEmail
  val developerFirstName = "Purnima"
  val developerLastName  = "Fakename"

  val unverifiedUser = RegisteredUser(
    email = MockDataSugar.developer8.toLaxEmail,
    userId = UserId(MockDataSugar.developer8Id),
    firstName = MockDataSugar.dev8FirstName,
    lastName = MockDataSugar.dev8LastName,
    verified = false,
    mfaDetails = List(AuthenticatorAppMfaDetail(MfaId.random, "Some app", Instant.now(), verified = true))
  )

  val unverifiedUserJson = Json.toJson(unverifiedUser).toString

}
