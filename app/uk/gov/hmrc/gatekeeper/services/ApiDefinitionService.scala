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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiDefinition
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.connectors.ApmConnector

@Singleton
class ApiDefinitionService @Inject() (
    apmConnector: ApmConnector
  )(implicit ec: ExecutionContext
  ) {

  def fetchAllApiDefinitions(environment: Option[Environment] = None)(implicit hc: HeaderCarrier): Future[List[ApiDefinition]] = {
    val envs = environment.fold[List[Environment]](Environment.values.toList)(List(_))

    val allApis: List[Future[List[ApiDefinition]]] = envs.map(e => apmConnector.fetchAllApis(e))

    Future.reduceLeft(allApis)(_ ++ _)
      .map(_.distinct) // for when identical in both environments
  }

  def apis(implicit hc: HeaderCarrier): Future[List[(ApiDefinition, Environment)]] = {
    val envs                                                                                   = Environment.values.toList
    val tupleEnv: Environment => ApiDefinition => (ApiDefinition, Environment)                 = (e) => (a) => (a, e)
    val tupleListEnv: Environment => List[ApiDefinition] => List[(ApiDefinition, Environment)] = (e) => (as) => as.map(tupleEnv(e))

    Future.reduceLeft(
      envs.map(e =>
        apmConnector.fetchAllApis(e)
          .map(tupleListEnv(e))
      )
    )(_ ++ _)
      .map(_.sortBy { case (api, env) => (api.name, env.toString()) })
  }
}
