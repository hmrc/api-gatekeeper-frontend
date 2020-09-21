/*
 * Copyright 2020 HM Revenue & Customs
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

package builder

import model.APIAccessType
import model.APIAccessType._
import model.APIAccess
import model.APIStatus
import model.APIStatus._
import model.subscriptions.VersionData
import model.subscriptions.ApiData
import model.ApiContext
import model.ApiVersion

trait ApiBuilder {

  def buildApiAccess(accessType: APIAccessType = APIAccessType.PUBLIC) = APIAccess(accessType)

  def buildApiVersionData(status: APIStatus = APIStatus.BETA, apiAccess: APIAccess = buildApiAccess()) = VersionData(status, apiAccess)

  def buildApiData(serviceName: String = "helloworld", isTestSupport: Boolean = false, versionData: VersionData = buildApiVersionData()): ApiData = {
    ApiData(serviceName, serviceName, isTestSupport, Map(ApiVersion.random -> versionData))
  }

  def builAapiContextAndApiData(apiContext: ApiContext = ApiContext.random, apiData: ApiData = buildApiData()) = Map(apiContext -> apiData)

}