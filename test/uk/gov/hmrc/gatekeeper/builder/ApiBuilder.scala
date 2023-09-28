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

package uk.gov.hmrc.gatekeeper.builder

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.gatekeeper.models.subscriptions.{ApiData, VersionData}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApiVersionNbr

trait ApiBuilder {

  implicit class VersionDataExtension(versionData: VersionData) {
    def withStatus(newStatus: ApiStatus) = versionData.copy(status = newStatus)
    def alpha                            = versionData.copy(status = ApiStatus.ALPHA)
    def beta                             = versionData.copy(status = ApiStatus.BETA)
    def stable                           = versionData.copy(status = ApiStatus.STABLE)
    def deprecated                       = versionData.copy(status = ApiStatus.DEPRECATED)
    def retired                          = versionData.copy(status = ApiStatus.RETIRED)

    // def withAccess(newAccessType: ApiAccessType) = versionData.copy(access = versionData.access.copy(`type` = newAccessType))
    def withAccess(newAccess: ApiAccess) = versionData.copy(access = newAccess)
    def publicAccess                             = this.withAccess(ApiAccess.PUBLIC)
    def privateAccess                            = this.withAccess(ApiAccess.Private(false))
  }

  implicit class ApiDataExtension(apiData: ApiData) {
    def testSupport = apiData.copy(isTestSupport = true)

    def withName(newName: String) = apiData.copy(name = newName)

    def withVersion(versionNbr: ApiVersionNbr, data: VersionData = DefaultVersionData) = apiData.copy(versions = Map(versionNbr -> data))

    def addVersion(versionNbr: ApiVersionNbr, data: VersionData = DefaultVersionData) = apiData.copy(versions = apiData.versions + (versionNbr -> data))
  }

  val DefaultVersionData = VersionData(status = ApiStatus.STABLE, access = ApiAccess.PUBLIC)

  val DefaultServiceName = "A-Service"
  val DefaultName        = "API Name"

  val VersionOne   = ApiVersionNbr("1.0")
  val VersionTwo   = ApiVersionNbr("2.0")
  val VersionThree = ApiVersionNbr("3.0")

  val DefaultApiData = ApiData(
    serviceName = DefaultServiceName,
    name = DefaultName,
    isTestSupport = false,
    versions = Map(VersionOne -> DefaultVersionData)
  )
}
