/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.services

import uk.gov.hmrc.gatekeeper.connectors.{ApiDefinitionConnector, ProductionApiDefinitionConnector, SandboxApiDefinitionConnector}
import javax.inject.Inject
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models.{APICategoryDetails, ApiDefinition}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class ApiDefinitionService @Inject()(sandboxApiDefinitionConnector: SandboxApiDefinitionConnector,
                                     productionApiDefinitionConnector: ProductionApiDefinitionConnector)(implicit ec: ExecutionContext) {

  def fetchAllApiDefinitions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val connectors = environment match {
      case Some(PRODUCTION) => List(productionApiDefinitionConnector)
      case Some(SANDBOX) => List(sandboxApiDefinitionConnector)
      case _ => List(sandboxApiDefinitionConnector, productionApiDefinitionConnector)
    }
    val publicApisFuture: List[Future[List[ApiDefinition]]] = connectors.map(_.fetchPublic())
    val privateApisFuture = connectors.map(_.fetchPrivate())

    for {
      publicApis <- Future.reduceLeft(publicApisFuture)(_ ++ _)
      privateApis <- Future.reduceLeft(privateApisFuture)(_ ++ _)
    } yield (publicApis ++ privateApis).distinct
  }

  def fetchAllDistinctApisIgnoreVersions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    fetchAllApiDefinitions(environment).map(_.groupBy(_.serviceName).map(_._2.head).toList)
  }

  def apis(implicit hc: HeaderCarrier) : Future[List[(ApiDefinition, Environment)]] = {
    
    def getApisFromConnector(connector: ApiDefinitionConnector) : Future[List[(ApiDefinition, Environment)]] = {
      def addEnvironmentToApis(result: Future[List[ApiDefinition]]) : Future[List[(ApiDefinition, Environment)]] =
        result.map(apis => apis.map(api => (api, connector.environment)))

      Future.sequence(
        List(
          connector.fetchPublic(),
          connector.fetchPrivate()
        ).map(addEnvironmentToApis)
      ).map(_.flatten)
    }

    val connectors = List(sandboxApiDefinitionConnector, productionApiDefinitionConnector)

    Future.sequence(connectors
      .map(getApisFromConnector))
      .map(_.flatten)
      .map(_.sortBy { case (api, env) => (api.name, env) } )
  }

  def apiCategories()(implicit hc: HeaderCarrier) : Future[List[APICategoryDetails]] = {
    for{
      sandboxCategories <- sandboxApiDefinitionConnector.fetchAPICategories()
      productionCategories <- productionApiDefinitionConnector.fetchAPICategories()
    } yield( sandboxCategories ++ productionCategories).distinct
  }


}
