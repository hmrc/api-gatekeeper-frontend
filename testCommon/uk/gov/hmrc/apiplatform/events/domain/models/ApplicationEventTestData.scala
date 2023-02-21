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

package uk.gov.hmrc.apiplatform.modules.events.domain.models

import java.time.{Clock, Instant, ZoneOffset}
import java.util.UUID
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.TermsAndConditionsLocations
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.PrivacyPolicyLocations
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress}
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.apiplatform.modules.submissions.domain.models.SubmissionId

trait ApplicationEventTestData {
  
  val fixedClock: Clock = Clock.fixed(Instant.now, ZoneOffset.UTC)


  val appCollaboratorActor: Actors.AppCollaborator = Actors.AppCollaborator(LaxEmailAddress("iam@admin.com"))
  val gkActor: Actors.GatekeeperUser = Actors.GatekeeperUser("iam@admin.com")
  val nowInstant: Instant = Instant.now(fixedClock)
  val userId = UserId(UUID.randomUUID())

}
