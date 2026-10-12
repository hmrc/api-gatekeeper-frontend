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
import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

import com.google.common.base.Charsets
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.{JsonFraming, Sink, Source}
import org.apache.pekko.util.ByteString

import play.api.libs.json.{Json, OFormat, Reads}
import play.api.libs.ws.JsonBodyWritables
import play.mvc.Http
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, *}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationWithCollaborators
import uk.gov.hmrc.apiplatform.modules.applications.core.interface.models.*
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.models.{ApplicationQuery, ParamName}
import uk.gov.hmrc.apiplatform.modules.applications.query.domain.services.QueryParamsToQueryStringMap
import uk.gov.hmrc.apiplatform.modules.common.domain.models.*

case class ApplicationsByRequest(emails: List[LaxEmailAddress])

object ApplicationsByRequest {
  given format: OFormat[ApplicationsByRequest] = Json.format[ApplicationsByRequest]
}

@Singleton
class ThirdPartyOrchestratorConnector @Inject() (http: HttpClientV2, config: ThirdPartyOrchestratorConnector.Config)(using ec: ExecutionContext, mat: Materializer)
    extends JsonBodyWritables {

  def getApplication(applicationId: ApplicationId)(using hc: HeaderCarrier): Future[Option[ApplicationWithCollaborators]] = {
    http.get(url"${config.serviceBaseUrl}/applications/$applicationId").execute[Option[ApplicationWithCollaborators]]
  }

  def getApplicationsByEmails(emails: List[LaxEmailAddress])(using hc: HeaderCarrier): Future[List[ApplicationWithCollaborators]] = {
    http.post(url"${config.serviceBaseUrl}/developer/applications")
      .withBody(Json.toJson(ApplicationsByRequest(emails)))
      .execute[List[ApplicationWithCollaborators]]
  }

  def validateName(name: String, selfApplicationId: Option[ApplicationId], environment: Environment)(using hc: HeaderCarrier): Future[ApplicationNameValidationResult] = {

    val body = selfApplicationId.fold[ApplicationNameValidationRequest](NewApplicationNameValidationRequest(name))(appId => ChangeApplicationNameValidationRequest(name, appId))

    http.post(url"${config.serviceBaseUrl}/environment/$environment/application/name/validate")
      .withBody(Json.toJson[ApplicationNameValidationRequest](body))
      .execute[Option[ApplicationNameValidationResult]]
      .map {
        case Some(x) => x
        case None    => throw new RuntimeException // ApplicationNotFound
      }
  }

  def query[T](environment: Environment)(qry: ApplicationQuery)(using hc: HeaderCarrier, rds: HttpReads[T]): Future[T] = {

    val qryStringMap = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }

    rawQuery[T](environment)(qryStringMap)
  }

  def rawQuery[T](environment: Environment)(qryStringMap: Map[ParamName, String])(using hc: HeaderCarrier, rds: HttpReads[T]): Future[T] = {
    val rawQryStringMap = qryStringMap.map {
      case (k, v) => (k.text -> v)
    }
    http
      .get(url"${config.serviceBaseUrl}/environment/$environment/query?$rawQryStringMap")
      .execute[T]
  }

  def queryStream[S, T](environment: Environment)(qry: ApplicationQuery)(fn: S => T)(using hc: HeaderCarrier, rds: Reads[S]): Future[List[T]] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }
    rawQueryStream[S, T](environment)(params)(fn)
  }

  def queryStream[S](environment: Environment)(qry: ApplicationQuery)(using hc: HeaderCarrier, rds: Reads[S]): Future[List[S]] = {
    val params = QueryParamsToQueryStringMap.toQuery(qry).map {
      case (k, vs) => k -> vs.mkString
    }
    rawQueryStream[S, S](environment)(params)(identity)
  }

  def rawQueryStream[S, T](environment: Environment)(qryStringMap: Map[ParamName, String])(fn: S => T)(using hc: HeaderCarrier, rds: Reads[S]): Future[List[T]] = {
    val rawQryStringMap = qryStringMap.map {
      case (k, v) => (k.text -> v)
    }
    import uk.gov.hmrc.http.client.readSource

    http
      .get(url"${config.serviceBaseUrl}/environment/$environment/query?$rawQryStringMap")
      .transform(_.withRequestTimeout(1.minutes))
      .setHeader(Http.HeaderNames.ACCEPT -> "application/stream+json")
      .stream[Source[ByteString, ?]]
      .map {
        _.via(JsonFraming.objectScanner(maximumObjectLength = Int.MaxValue))
          .map { bytestring =>
            Json.fromJson[S](Json.parse(bytestring.decodeString(Charsets.UTF_8))).asOpt
          }
          .collect {
            case Some(s) => fn(s)
          }
      }
      .flatMap(_.runWith(Sink.seq))
      .map(_.toList)
  }
}

object ThirdPartyOrchestratorConnector {
  case class Config(serviceBaseUrl: String)
}
