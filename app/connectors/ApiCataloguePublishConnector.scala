/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.{Inject, Singleton}
import config.AppConfig
import model.DeveloperStatusFilter.DeveloperStatusFilter
import model._
import model.TopicOptionChoice.TopicOptionChoice
import encryption._
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.http.HttpClient
import model.UserId
import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.name.Named
import uk.gov.hmrc.http.HttpReads.Implicits._
import cats.data.OptionT
import scala.util.control.NonFatal
import play.api.Logger



@Singleton
class ApiCataloguePublishConnector @Inject()(appConfig: ApiCataloguePublishConnector.Config, http: HttpClient)
    (implicit ec: ExecutionContext) {

  import ApiCataloguePublishConnector._

  def publishByServiceName(serviceName: String)(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishResponse]] = {
    implicit val f = PublishResponse.formatPublishResponse
    handleResult(http.POST[String, PublishResponse](s"${appConfig.serviceBaseUrl}/api-platform-api-catalogue-publish/publish/$serviceName", ""))
    
  }

  def publishAll()(implicit hc: HeaderCarrier): Future[Either[Throwable, PublishAllResponse]] = {
    implicit val f = PublishAllResponse.formatPublishAllResponse
    handleResult(http.POSTEmpty[PublishAllResponse](s"${appConfig.serviceBaseUrl}/api-platform-api-catalogue-publish/publish-all", Seq.empty))
    
  }

  private def handleResult[A](result: Future[A]): Future[Either[Throwable, A]] ={
    result.map(x=> Right(x))
      .recover {
        case NonFatal(e) => Logger.error(e.getMessage)
          Left(e)
      }
    }

}

object ApiCataloguePublishConnector {

  case class PublishResponse(id: String, publisherReference: String, platformType: String)
  case class PublishAllResponse(message: String)

  object PublishResponse {
    implicit val formatPublishResponse = Json.format[PublishResponse]
  
  }
  object PublishAllResponse {
    implicit val formatPublishAllResponse = Json.format[PublishAllResponse]
  }

  case class Config(serviceBaseUrl: String)
}
