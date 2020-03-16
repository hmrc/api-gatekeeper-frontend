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

package connectors

import java.net.URLEncoder.encode
import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import javax.inject.{Inject, Singleton}
import model.Environment.Environment
import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, _}
import model._
import play.api.Logger
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{Format, Json}
import services.SubscriptionFieldsService.{DefinitionsByApiVersion, SubscriptionFieldsConnector}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.Retries

import scala.concurrent.{ExecutionContext, Future}

abstract class AbstractSubscriptionFieldsConnector(implicit ec: ExecutionContext) extends SubscriptionFieldsConnector with Retries {
  protected val httpClient: HttpClient
  protected val proxiedHttpClient: ProxiedHttpClient
  val environment: Environment
  val serviceBaseUrl: String
  val useProxy: Boolean
  val bearerToken: String
  val apiKey: String

  import SubscriptionFieldsConnector.JsonFormatters._
  import SubscriptionFieldsConnector._

  def http: HttpClient = if (useProxy) proxiedHttpClient.withHeaders(bearerToken, apiKey) else httpClient

  def fetchFieldsValuesWithPrefetchedDefinitions(clientId: String, apiContextVersion: ApiContextVersion, definitionsCache: DefinitionsByApiVersion)
                                                (implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {
    def joinFieldValuesToDefinitions(defs: Seq[SubscriptionFieldDefinition], fieldValues: Fields): Seq[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.getOrElse(field.name, "")))
    }

    def ifDefinitionsGetValues(definitions: Seq[SubscriptionFieldDefinition]): Future[Option[ApplicationApiFieldValues]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      }
      else {
        fetchApplicationApiValues(clientId, apiContextVersion.context, apiContextVersion.version)
      }
    }

    val definitions: Seq[SubscriptionFieldDefinition] = definitionsCache.getOrElse(apiContextVersion,Seq.empty)

    for {
      subscriptionFields <- ifDefinitionsGetValues(definitions)
      fieldValues = subscriptionFields.fold(Fields.empty)(_.fields)
    } yield joinFieldValuesToDefinitions(definitions, fieldValues)
  }

//  private def urlEncode(str: String, encoding: String = "UTF-8") = {
//    encode(str, encoding)
//  }
//
//  private def urlSubscriptionFieldValues(clientId: String, apiContext: String, apiVersion: String) =
//    s"$serviceBaseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  def fetchFieldValues(clientId: String, context: String, version: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldValue]] = {

    def joinFieldValuesToDefinitions(defs: Seq[SubscriptionFieldDefinition], fieldValues: Fields): Seq[SubscriptionFieldValue] = {
      defs.map(field => SubscriptionFieldValue(field, fieldValues.getOrElse(field.name, "")))
    }

    def ifDefinitionsGetValues(definitions: Seq[SubscriptionFieldDefinition]): Future[Option[ApplicationApiFieldValues]] = {
      if (definitions.isEmpty) {
        Future.successful(None)
      }
      else {
        fetchApplicationApiValues(clientId, context, version)
      }
    }

    for {
      definitions <- fetchFieldDefinitions(context, version)
      subscriptionFields <- ifDefinitionsGetValues(definitions)
      fieldValues = subscriptionFields.fold(Fields.empty)(_.fields)
    } yield joinFieldValuesToDefinitions(definitions, fieldValues)
  }

  def fetchFieldDefinitions(apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[Seq[SubscriptionFieldDefinition]] = {
    val url = urlSubscriptionFieldDefinition(apiContext, apiVersion)
    Logger.debug(s"fetchFieldDefinitions() - About to call $url in ${environment.toString}")
    retry {
      http.GET[ApiFieldDefinitions](url).map(response => response.fieldDefinitions.map(toDomain))
    } recover recovery(Seq.empty[SubscriptionFieldDefinition])
  }


  def fetchAllFieldDefinitions()(implicit hc: HeaderCarrier): Future[DefinitionsByApiVersion] = {
    val url = s"$serviceBaseUrl/definition"
    retry {
      for {
        response <- http.GET[AllApiFieldDefinitions](url)
      } yield toDomain(response)

    } recover recovery(DefinitionsByApiVersion.empty)
  }

  def saveFieldValues(clientId: String, apiContext: String, apiVersion: String, fields: Fields)(implicit hc: HeaderCarrier): Future[HttpResponse] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.PUT[SubscriptionFieldsPutRequest, HttpResponse](url, SubscriptionFieldsPutRequest(clientId, apiContext, apiVersion, fields))
  }

  def deleteFieldValues(clientId: String, apiContext: String, apiVersion: String)(implicit hc: HeaderCarrier): Future[FieldsDeleteResult] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    http.DELETE[HttpResponse](url).map { response =>
      response.status match {
        case NO_CONTENT => FieldsDeleteSuccessResult
        case _ => FieldsDeleteFailureResult
      }
    } recover {
      case _: NotFoundException => FieldsDeleteSuccessResult
      case _ => FieldsDeleteFailureResult
    }
  }

  private def fetchApplicationApiValues(clientId: String, apiContext: String, apiVersion: String)
                                       (implicit hc: HeaderCarrier): Future[Option[ApplicationApiFieldValues]] = {
    val url = urlSubscriptionFieldValues(clientId, apiContext, apiVersion)
    retry {
      http.GET[ApplicationApiFieldValues](url).map(Some(_))
    } recover recovery(None)
  }

  private def urlEncode(str: String, encoding: String = "UTF-8") = encode(str, encoding)

  private def urlSubscriptionFieldValues(clientId: String, apiContext: String, apiVersion: String) =
    s"$serviceBaseUrl/field/application/${urlEncode(clientId)}/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def urlSubscriptionFieldDefinition(apiContext: String, apiVersion: String) =
    s"$serviceBaseUrl/definition/context/${urlEncode(apiContext)}/version/${urlEncode(apiVersion)}"

  private def recovery[T](value: T): PartialFunction[Throwable, T] = {
    case _: NotFoundException => value
  }
}

object SubscriptionFieldsConnector {

  def toDomain(f: FieldDefinition): SubscriptionFieldDefinition = {
    SubscriptionFieldDefinition(
      name = f.name,
      description = f.description,
      `type` = f.`type`,
      hint = f.hint
    )
  }

  def toDomain(fs: AllApiFieldDefinitions): DefinitionsByApiVersion = {
    fs.apis.map( fd =>
      ApiContextVersion(fd.apiContext, fd.apiVersion) -> fd.fieldDefinitions.map(toDomain)
    )
    .toMap
  }

  private[connectors] case class ApplicationApiFieldValues(clientId: String, apiContext: String, apiVersion: String, fieldsId: UUID, fields: Map[String, String])

  private[connectors] case class FieldDefinition(name: String, description: String, hint: String, `type`: String)

  private[connectors] case class ApiFieldDefinitions(apiContext: String, apiVersion: String, fieldDefinitions: List[FieldDefinition])

  private[connectors] case class AllApiFieldDefinitions(apis: Seq[ApiFieldDefinitions])

  object JsonFormatters {
    implicit val format: Format[ApplicationApiFieldValues] = Json.format[ApplicationApiFieldValues]
    implicit val formatFieldDefinition: Format[FieldDefinition] = Json.format[FieldDefinition]
    implicit val formatApiFieldDefinitionsResponse: Format[ApiFieldDefinitions] = Json.format[ApiFieldDefinitions]
    implicit val formatAllApiFieldDefinitionsResponse: Format[AllApiFieldDefinitions] = Json.format[AllApiFieldDefinitions]
  }
}

@Singleton
class SandboxSubscriptionFieldsConnector @Inject()(val appConfig: AppConfig,
                                                   val httpClient: HttpClient,
                                                   val proxiedHttpClient: ProxiedHttpClient,
                                                   val actorSystem: ActorSystem,
                                                   val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.SANDBOX
  val serviceBaseUrl: String = appConfig.subscriptionFieldsSandboxBaseUrl
  val useProxy: Boolean = appConfig.subscriptionFieldsSandboxUseProxy
  val bearerToken: String = appConfig.subscriptionFieldsSandboxBearerToken
  val apiKey: String = appConfig.subscriptionFieldsSandboxApiKey
}

@Singleton
class ProductionSubscriptionFieldsConnector @Inject()(val appConfig: AppConfig,
                                                      val httpClient: HttpClient,
                                                      val proxiedHttpClient: ProxiedHttpClient,
                                                      val actorSystem: ActorSystem,
                                                      val futureTimeout: FutureTimeoutSupport)(implicit val ec: ExecutionContext)
  extends AbstractSubscriptionFieldsConnector {

  val environment: Environment = Environment.PRODUCTION
  val serviceBaseUrl: String = appConfig.subscriptionFieldsProductionBaseUrl
  val useProxy: Boolean = appConfig.subscriptionFieldsProductionUseProxy
  val bearerToken: String = appConfig.subscriptionFieldsProductionBearerToken
  val apiKey: String = appConfig.subscriptionFieldsProductionApiKey
}
