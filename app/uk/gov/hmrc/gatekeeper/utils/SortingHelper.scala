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

import uk.gov.hmrc.gatekeeper.models.VersionSubscriptionWithoutFields
import uk.gov.hmrc.gatekeeper.models.VersionSubscription
import uk.gov.hmrc.gatekeeper.models.ApiVersionDefinition
import scala.util.Try

object SortingHelper {

  private val nonNumericOrPeriodRegex = "[^\\d^.]*"
  private val fallback                = Array(1, 0, 0)

  private def versionSorter(v1: ApiVersionDefinition, v2: ApiVersionDefinition) = {
    val v1Parts = Try(v1.version.value.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val v2Parts = Try(v2.version.value.replaceAll(nonNumericOrPeriodRegex, "").split("\\.").map(_.toInt)).getOrElse(fallback)
    val pairs   = v1Parts.zip(v2Parts)

    val firstUnequalPair = pairs.find { case (one, two) => one != two }
    firstUnequalPair.fold(v1.version.value.length > v2.version.value.length) { case (a, b) => a > b }
  }

  def descendingVersionWithoutFields(v1: VersionSubscriptionWithoutFields, v2: VersionSubscriptionWithoutFields) = {
    versionSorter(v1.version, v2.version)
  }

  def descendingVersionWithFields(v1: VersionSubscription, v2: VersionSubscription) = {
    versionSorter(v1.version, v2.version)
  }

}
