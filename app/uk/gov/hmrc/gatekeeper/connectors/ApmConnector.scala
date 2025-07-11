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

package uk.gov.hmrc.gatekeeper.connectors

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext}
import uk.gov.hmrc.http.client.HttpClientV2

object ApmConnector {
  case class Config(
    serviceBaseUrl: String
  )
}

trait ApmConnectorModule {
  def http: HttpClientV2
  def config: ApmConnector.Config
  implicit def ec: ExecutionContext
}

@Singleton
class ApmConnector @Inject() (val http: HttpClientV2, val config: ApmConnector.Config)(implicit val ec: ExecutionContext)
    extends ApmConnectoCombinedApisModule 
    with ApmConnectorApiDefinitionModule
    with ApmConnectorApplicationModule
    with ApmConnectorPpnsModule
    with ApmConnectorSubscriptionFieldsModule {
}
