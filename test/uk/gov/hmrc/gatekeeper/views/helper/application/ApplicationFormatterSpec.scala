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

package uk.gov.hmrc.gatekeeper.views.helper.application

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}

import org.scalatest.BeforeAndAfterAll

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.views.helper.application.ApplicationFormatter.{getCreatedOn, initialLastAccessDate}

class ApplicationFormatterSpec extends AsyncHmrcSpec with BeforeAndAfterAll with ApplicationBuilder {
  val FixedTimeNow: LocalDateTime = LocalDateTime.of(2019, 9, 1, 0, 30, 0, 0)

  val applicationId = ApplicationId.random

  "getCreatedOn" should {
    "return the createdOn value with long date format" in {
      val createdOn = LocalDateTime.of(2019, 1, 1, 0, 0, 0, 0).toInstant(ZoneOffset.UTC)
      getCreatedOn(DefaultApplication.withCreatedOn(createdOn)) shouldBe "01 January 2019"
    }
  }
}
