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
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.connectors.ApiCataloguePublishConnector._

@Singleton
class ApiCataloguePublishConnector @Inject() (appConfig: ApiCataloguePublishConnector.Config, http: HttpClientV2)(implicit ec: ExecutionContext) extends ApplicationLogger {

  def publishByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResponse]] =
    handleResult(http.post(url"${appConfig.serviceBaseUrl}/api-platform-api-catalogue-publish/publish/$serviceName")
      .execute[PublishResponse])

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] = {
    result.map(x => Right(x))
      .recover {
        case NonFatal(e) =>
          logger.error(e.getMessage)
          Left(e)
      }
  }

}

object ApiCataloguePublishConnector {
  case class Config(serviceBaseUrl: String)
  // API Catalogue Publish
  case class PublishResponse(id: String, publisherReference: String, platformType: String)
  implicit val formatPublishResponse: OFormat[PublishResponse] = Json.format[PublishResponse]
}
