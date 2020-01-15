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

package services

import javax.inject.Inject
import connectors.{ProductionApiDefinitionConnector, SandboxApiDefinitionConnector}
import model.APIDefinition
import model.Environment._

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.http.HeaderCarrier

class ApiDefinitionService @Inject()(sandboxApiDefinitionConnector: SandboxApiDefinitionConnector,
                                     productionApiDefinitionConnector: ProductionApiDefinitionConnector)(implicit ec: ExecutionContext) {

  def fetchAllApiDefinitions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    val connectors = environment match {
      case Some(PRODUCTION) => Seq(productionApiDefinitionConnector)
      case Some(SANDBOX) => Seq(sandboxApiDefinitionConnector)
      case _ => Seq(sandboxApiDefinitionConnector, productionApiDefinitionConnector)
    }
    val publicApisFuture = connectors.map(_.fetchPublic())
    val privateApisFuture = connectors.map(_.fetchPrivate())

    for {
      publicApis <- Future.reduce(publicApisFuture)(_ ++ _)
      privateApis <- Future.reduce(privateApisFuture)(_ ++ _)
    } yield (publicApis ++ privateApis).distinct
  }
}
