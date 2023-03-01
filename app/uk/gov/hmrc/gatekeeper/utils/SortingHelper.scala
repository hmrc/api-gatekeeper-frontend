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

package uk.gov.hmrc.gatekeeper.utils

import scala.util.Try
import uk.gov.hmrc.gatekeeper.models.VersionSubscriptionWithoutFields
import uk.gov.hmrc.gatekeeper.models.VersionSubscription

object SortingHelper {

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback                = Array(1, 0, 0)

  def versionSorter(v1: String, v2: String) = {
    val v1Parts = Try(v1.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs   = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.length > v2.length) { case (a, b) => a > b }
  }

  def descendingVersionWithoutFields(v1: VersionSubscriptionWithoutFields, v2: VersionSubscriptionWithoutFields) = {
    versionSorter(v1.version.version.value, v2.version.version.value)
  }

  def descendingVersionWithFields(v1: VersionSubscription, v2: VersionSubscription) = {
    versionSorter(v1.version.version.value, v2.version.version.value)
  }

}
