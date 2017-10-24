/*
 * Copyright 2017 HM Revenue & Customs
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

package connectors

import config.WSHttp
import model._
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.{ HeaderCarrier, HttpGet, Upstream5xxResponse }

object ApiDefinitionConnector extends ApiDefinitionConnector with ServicesConfig {
  override val serviceBaseUrl = baseUrl("api-definition")
  override val http = WSHttp
}

trait ApiDefinitionConnector {
  val serviceBaseUrl: String
  val http: HttpGet

  def fetchPublic()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/api-definition")
      .recover {
        case _: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }

  def fetchPrivate()(implicit hc: HeaderCarrier): Future[Seq[APIDefinition]] = {
    http.GET[Seq[APIDefinition]](s"$serviceBaseUrl/api-definition?type=private")
      .recover {
        case _: Upstream5xxResponse => throw new FetchApiDefinitionsFailed
      }
  }
}
