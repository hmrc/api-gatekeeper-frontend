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

package uk.gov.hmrc.gatekeeper.services

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.gatekeeper.connectors.{ApiDefinitionConnector, ProductionApiDefinitionConnector, SandboxApiDefinitionConnector}
import uk.gov.hmrc.gatekeeper.models.Environment._
import uk.gov.hmrc.gatekeeper.models.{ApiDefinitionGK}

class ApiDefinitionService @Inject() (
    sandboxApiDefinitionConnector: SandboxApiDefinitionConnector,
    productionApiDefinitionConnector: ProductionApiDefinitionConnector
  )(implicit ec: ExecutionContext
  ) {

  def fetchAllApiDefinitions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[List[ApiDefinitionGK]] = {
    val connectors                                          = environment match {
      case Some(PRODUCTION) => List(productionApiDefinitionConnector)
      case Some(SANDBOX)    => List(sandboxApiDefinitionConnector)
      case _                => List(sandboxApiDefinitionConnector, productionApiDefinitionConnector)
    }
    val publicApisFuture: List[Future[List[ApiDefinitionGK]]] = connectors.map(_.fetchPublic())
    val privateApisFuture                                   = connectors.map(_.fetchPrivate())

    for {
      publicApis  <- Future.reduceLeft(publicApisFuture)(_ ++ _)
      privateApis <- Future.reduceLeft(privateApisFuture)(_ ++ _)
    } yield (publicApis ++ privateApis).distinct
  }

  def fetchAllDistinctApisIgnoreVersions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[List[ApiDefinitionGK]] = {
    fetchAllApiDefinitions(environment).map(_.groupBy(_.serviceName).map(_._2.head).toList)
  }

  def apis(implicit hc: HeaderCarrier): Future[List[(ApiDefinitionGK, Environment)]] = {

    def getApisFromConnector(connector: ApiDefinitionConnector): Future[List[(ApiDefinitionGK, Environment)]] = {
      def addEnvironmentToApis(result: Future[List[ApiDefinitionGK]]): Future[List[(ApiDefinitionGK, Environment)]] =
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
      .map(_.sortBy { case (api, env) => (api.name, env) })
  }
}
