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

import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.ApiStatus._
import uk.gov.hmrc.gatekeeper.models.subscriptions.VersionData
import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.gatekeeper.models.subscriptions.ApiData

trait ApiBuilder {

  implicit class VersionDataExtension(versionData: VersionData) {
    def withStatus(newStatus: ApiStatus) = versionData.copy(status = newStatus)
    def alpha                            = versionData.copy(status = ApiStatus.ALPHA)
    def beta                             = versionData.copy(status = ApiStatus.BETA)
    def stable                           = versionData.copy(status = ApiStatus.STABLE)
    def deprecated                       = versionData.copy(status = ApiStatus.DEPRECATED)
    def retired                          = versionData.copy(status = ApiStatus.RETIRED)

    def withAccess(newAccessType: APIAccessType) = versionData.copy(access = versionData.access.copy(`type` = newAccessType))
    def publicAccess                             = this.withAccess(APIAccessType.PUBLIC)
    def privateAccess                            = this.withAccess(APIAccessType.PRIVATE)
  }

  implicit class ApiDataExtension(apiData: ApiData) {
    def testSupport = apiData.copy(isTestSupport = true)

    def withName(newName: String) = apiData.copy(name = newName)

    def withVersion(version: ApiVersion, data: VersionData = DefaultVersionData) = apiData.copy(versions = Map(version -> data))

    def addVersion(version: ApiVersion, data: VersionData = DefaultVersionData) = apiData.copy(versions = apiData.versions + (version -> data))
  }

  val DefaultVersionData = VersionData(status = STABLE, access = ApiAccess(`type` = APIAccessType.PUBLIC))

  val DefaultServiceName = "A-Service"
  val DefaultName        = "API Name"

  val VersionOne   = ApiVersion("1.0")
  val VersionTwo   = ApiVersion("2.0")
  val VersionThree = ApiVersion("3.0")

  val DefaultApiData = ApiData(
    serviceName = DefaultServiceName,
    name = DefaultName,
    isTestSupport = false,
    versions = Map(VersionOne -> DefaultVersionData)
  )
}
