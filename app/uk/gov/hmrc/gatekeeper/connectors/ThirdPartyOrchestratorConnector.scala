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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, _}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models._
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.ApplicationQuery
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.services.QueryParamsToQueryStringMap
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.Materializer
import uk.gov.hmrc.http.client.StreamHttpReads
import org.apache.pekko.stream.scaladsl.JsonFraming
import org.apache.pekko.stream.scaladsl.Sink
import play.api.libs.json.Reads
import com.google.common.base.Charsets

case class ApplicationsByRequest(emails: List[LaxEmailAddress])

object ApplicationsByRequest {
  implicit val format: OFormat[ApplicationsByRequest] = Json.format[ApplicationsByRequest]
}

@Singleton
class ThirdPartyOrchestratorConnector @Inject() (http: HttpClientV2, config: ThirdPartyOrchestratorConnector.Config)(implicit ec: ExecutionContext, mat: Materializer) {

  def getApplication(applicationId: ApplicationId)(implicit hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    http.get(url"${config.serviceBaseUrl}/applications/$applicationId").execute[Option[ApplicationWithCollaborators]]
  }

  def getApplicationsByEmails(emails: List[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    http.post(url"${config.serviceBaseUrl}/developer/applications")
      .withBody(Json.toJson(ApplicationsByRequest(emails)))
      .execute[List[ApplicationWithCollaborators]]
  }

  def validateName(name: String, selfApplicationId: Option[ApplicationId], environment: Environment)(implicit hc: HeaderCarrier): Future[ApplicationNameValidationResult] = {

    val body = selfApplicationId.fold[ApplicationNameValidationRequest](NewApplicationNameValidationRequest(name))(appId => ChangeApplicationNameValidationRequest(name, appId))

    http.post(url"${config.serviceBaseUrl}/environment/$environment/application/name/validate")
      .withBody(Json.toJson[ApplicationNameValidationRequest](body))
      .execute[Option[ApplicationNameValidationResult]]
      .map {
        case Some(x) => x
        case None    => throw new RuntimeException // ApplicationNotFound
      }
  }

  def query[T](environment: Environment)(qry: ApplicationQuery)(implicit hc: HeaderCarrier, rds: HttpReads[T]): Future[T] = {
    val qryStringMap = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }

    rawQuery[T](environment)(qryStringMap)
  }

  def rawQuery[T](environment: Environment)(qryStringMap: Map[String, String])(implicit hc: HeaderCarrier, rds: HttpReads[T]): Future[T] = {
    http
      .get(url"${config.serviceBaseUrl}/environment/$environment/query?$qryStringMap")
      .execute[T]
  }

  /* 
val response: Future[Source[String, _]] = wsClient.url("https://example.com")
  .stream()
  .map { response =>
    response.bodyAsSource
      .via(Framing.delimiter(ByteString("\n"), maximumFrameLength = 1024))
      .map(_.utf8String)
  }
 */

  def queryStream[S,T](environment: Environment)(qry: ApplicationQuery)(fn: S => T)(implicit hc: HeaderCarrier, rds: Reads[S]): Future[List[T]] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }
    rawQueryStream[S,T](environment)(params)(fn)
  }

  def queryStream[S](environment: Environment)(qry: ApplicationQuery)(implicit hc: HeaderCarrier, rds: Reads[S]): Future[List[S]] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }
    rawQueryStream[S,S](environment)(params)(identity)
  }

  def rawQueryStream[S,T](environment: Environment)(qryStringMap: Map[String, String])(fn: S => T)(implicit hc: HeaderCarrier, rds: Reads[S]): Future[List[T]] = {
    http
      .get(url"${config.serviceBaseUrl}/environment/$environment/query?$qryStringMap")
      .stream[Source[ByteString,_]]
      .map { response =>
        response.via(JsonFraming.objectScanner(maximumObjectLength = Int.MaxValue))
      }

      .map { _.map { bytestring =>
        Json.fromJson[S](Json.parse(bytestring.decodeString(Charsets.UTF_8)))
        .asOpt
      }}
      
      .map(_.collect {
        case Some(s) => s
      })

      .map(_.map(fn))

      .flatMap(_.runWith(Sink.seq))
      
      .map(_.toList)
  }
}

object ThirdPartyOrchestratorConnector {
  case class Config(serviceBaseUrl: String)
}
