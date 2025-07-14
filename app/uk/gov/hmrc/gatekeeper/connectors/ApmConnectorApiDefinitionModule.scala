/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.connectors

import scala.concurrent.Future

import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiDefinition, MappedApiDefinitions}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

object ApmConnectorApiDefinitionModule {
  val ApplicationIdQueryParam = "applicationId"
  val RestrictedQueryParam    = "restricted"
}

trait ApmConnectorApiDefinitionModule extends ApmConnectorModule {
  import ApmConnectorApiDefinitionModule._

  private[this] val baseUrl = s"${config.serviceBaseUrl}/api-definitions"

  def fetchAllPossibleSubscriptions(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val queryParams = Seq(
      ApplicationIdQueryParam -> applicationId.value.toString(),
      RestrictedQueryParam    -> "false"
    )
    http.get(url"${baseUrl}?$queryParams")
      .execute[MappedApiDefinitions]
      .map(_.wrapped.values.toList)
  }

  def fetchNonOpenApis(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.get(url"${baseUrl}/nonopen?environment=$environment")
      .execute[List[ApiDefinition]]
  }

  def fetchAllApis(environment: Environment)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    http.get(url"${baseUrl}/all?environment=$environment")
      .execute[MappedApiDefinitions]
      .map(_.wrapped.values.toList)
  }
}
