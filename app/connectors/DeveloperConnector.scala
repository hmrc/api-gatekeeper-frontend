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
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.name.Named

trait DeveloperConnector {
  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]]

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]]

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[User]]

  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategory: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier): Future[Seq[User]]

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult]

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User]
}

@Singleton
class HttpDeveloperConnector @Inject()(appConfig: AppConfig, http: HttpClient, @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption)
    (implicit ec: ExecutionContext) 
    extends DeveloperConnector
    with SendsSecretRequest {

  private val postHeaders: Seq[(String, String)] = Seq(CONTENT_TYPE -> JSON)

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    // TODO: APIS-4925 - Need new endpoint for either encrypted email or POST to use body.
    http.GET[User](s"${appConfig.developerBaseUrl}/developer", Seq("email" -> email)).recover {
      case e: NotFoundException => UnregisteredCollaborator(email)
    }
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    http.POST[JsValue, Seq[User]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", Json.toJson(emails), postHeaders)
  }

  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategories: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    val regimes: Seq[(String,String)] = maybeApiCategories.fold(Seq.empty[(String,String)])(regimes =>  regimes.flatMap(regime => Seq("regime" -> regime.value)))
    val queryParams =
      Seq("topic" -> topic.toString) ++ regimes ++
      maybeApis.fold(Seq.empty[(String,String)])(apis => apis.map(("service" -> _)))

    http.GET[Seq[User]](s"${appConfig.developerBaseUrl}/developers/email-preferences", queryParams)
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    http.GET[Seq[User]](s"${appConfig.developerBaseUrl}/developers/all")
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) = {
    http.POST[DeleteDeveloperRequest,HttpResponse](s"${appConfig.developerBaseUrl}/developer/delete", deleteDeveloperRequest, postHeaders)
      .map(response => response.status match {
        case NO_CONTENT => DeveloperDeleteSuccessResult
        case _ => DeveloperDeleteFailureResult
      })
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = {
    // TODO: APIS-4925 - Need new endpoint for either encrypted email or POST to use body.
    http.POST[JsValue, User](s"${appConfig.developerBaseUrl}/developer/$email/mfa/remove", Json.obj("removedBy" -> loggedInUser))
  }

  def searchDevelopers(maybeEmail: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    import model.SearchParameters

    val payload = SearchParameters(maybeEmail, Some(status.value))

    secretRequest(payload) { request =>
      http.POST[SecretRequest, Seq[User]](s"${appConfig.developerBaseUrl}/developers/search", request)
    }
  }
}

@Singleton
class DummyDeveloperConnector extends DeveloperConnector {
  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = Future.successful(UnregisteredCollaborator(email))

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def fetchAll()(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def fetchByEmailPreferences(topic: TopicOptionChoice, maybeApis: Option[Seq[String]] = None, maybeApiCategories: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) =
    Future.successful(DeveloperDeleteSuccessResult)

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = Future.successful(UnregisteredCollaborator(email))

  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]] = Future.successful(Seq.empty)
}
