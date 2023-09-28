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

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

import org.scalatest.BeforeAndAfterAll

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.common.utils.AsyncHmrcSpec
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.views.helper.application.ApplicationFormatter.{getCreatedOn, getLastAccess, initialLastAccessDate}

class ApplicationFormatterSpec extends AsyncHmrcSpec with BeforeAndAfterAll with ApplicationBuilder {
  val FixedTimeNow: LocalDateTime = LocalDateTime.of(2019, 9, 1, 0, 30, 0, 0)

  val applicationId = ApplicationId.random

  "getCreatedOn" should {
    "return the createdOn value with long date format" in {
      val createdOn = LocalDateTime.of(2019, 1, 1, 0, 0, 0, 0) // scalastyle:ignore magic.number
      getCreatedOn(DefaultApplication.withCreatedOn(createdOn)) shouldBe "01 January 2019"
    }
  }

  "getLastAccess" should {
    "return the lastAccess value with long date format for dates after the initial last access date" in {
      val lastAccessDate = initialLastAccessDate.plusDays(1)
      val createdOnDate  = lastAccessDate.minusHours(1)
      val app            = DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(lastAccessDate)
      getLastAccess(app)(FixedTimeNow) shouldBe "26 June 2019"
    }

    "use inexact format for dates before the initial last access date" in {
      val lastAccessDate = initialLastAccessDate.minusDays(1)
      val createdOnDate  = lastAccessDate.minusHours(1)
      val app            = DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(lastAccessDate)
      getLastAccess(app)(FixedTimeNow) shouldBe "More than 2 months ago"
    }

    "use inexact format for dates on the initial last access date" in {
      val lastAccessDate = initialLastAccessDate.plusHours(3)
      val createdOnDate  = lastAccessDate.minusHours(1)
      val app            = DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(lastAccessDate)
      getLastAccess(app)(FixedTimeNow) shouldBe "More than 2 months ago"
    }

    "display 'never used' if the last access date is the same as the created date" in {
      val createdOnDate = initialLastAccessDate.plusHours(3)
      val app           = DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(createdOnDate)
      getLastAccess(app)(FixedTimeNow) shouldBe "No API called"
    }

    "display 'never used' if the last access date is within a second of the created date" in {
      val createdOnDate = initialLastAccessDate.plusHours(3)
      getLastAccess(DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(createdOnDate.plus(900, ChronoUnit.MILLIS)))(FixedTimeNow) shouldBe "No API called"  // scalastyle:ignore magic.number
      getLastAccess(DefaultApplication.withCreatedOn(createdOnDate).withLastAccess(createdOnDate.minus(900, ChronoUnit.MILLIS)))(FixedTimeNow) shouldBe "No API called" // scalastyle:ignore magic.number
    }
  }
}
