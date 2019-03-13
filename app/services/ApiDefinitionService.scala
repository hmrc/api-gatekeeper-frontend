/*
 * Copyright 2019 HM Revenue & Customs
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

package services

import javax.inject.Inject

import connectors.{ProductionApiDefinitionConnector, SandboxApiDefinitionConnector}
import model.APIDefinition

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier

class ApiDefinitionService @Inject()(sandboxApiDefinitionConnector: SandboxApiDefinitionConnector,
                                     productionApiDefinitionConnector: ProductionApiDefinitionConnector){

  def fetchAllApiDefinitions(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    val sandboxPublicApisFuture = sandboxApiDefinitionConnector.fetchPublic()
    val productionPublicApisFuture = productionApiDefinitionConnector.fetchPublic()
    val sandboxPrivateApisFuture = sandboxApiDefinitionConnector.fetchPrivate()
    val productionPrivateApisFuture = productionApiDefinitionConnector.fetchPrivate()

    for {
      sandboxPublicApis <- sandboxPublicApisFuture
      sandboxPrivateApis <- sandboxPrivateApisFuture
      productionPublicApis <- productionPublicApisFuture
      productionPrivateApis <- productionPrivateApisFuture
    } yield (sandboxPublicApis ++ sandboxPrivateApis ++ productionPublicApis ++ productionPrivateApis).distinct
  }
}
