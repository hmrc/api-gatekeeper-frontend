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
import uk.gov.hmrc.http.{HttpResponse, HeaderCarrier}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import model.UserId
import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.name.Named

import uk.gov.hmrc.http.HttpReads.Implicits._

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

object DeveloperConnector {
  case class GetOrCreateUserIdRequest(email: String)
  implicit val GetOrCreateUserIdRequestWrite = Json.writes[GetOrCreateUserIdRequest]

  case class GetOrCreateUserIdResponse(userId: UserId)
  implicit val GetOrCreateUserIdResponseReads = Json.reads[GetOrCreateUserIdResponse]

  case class RemoveMfaRequest(removedBy: String)
  implicit val RemoveMfaRequestWrites = Json.writes[RemoveMfaRequest]
}
@Singleton
class HttpDeveloperConnector @Inject()(appConfig: AppConfig, http: HttpClient, @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption)
    (implicit ec: ExecutionContext) 
    extends DeveloperConnector
    with SendsSecretRequest {

  import DeveloperConnector._

  def getOrCreateUser(email: String)(implicit hc: HeaderCarrier): Future[UserId] = {
    http.POST[GetOrCreateUserIdRequest, GetOrCreateUserIdResponse](s"${appConfig.developerBaseUrl}/developer/user-id", GetOrCreateUserIdRequest(email)).map(_.userId)
  }
  

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      userId <- getOrCreateUser(email)
      // Beware !!!
      // This GET only looks at registered users and not unregistered users so we still need the _.getOrElse below.
      user <- http.GET[Option[User]](s"${appConfig.developerBaseUrl}/developer", Seq("developerId" -> userId.value.toString))
              .map(_.getOrElse(UnregisteredCollaborator(email)))
    } yield user
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    http.POST[Iterable[String], Seq[User]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", emails)
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
    http.POST[DeleteDeveloperRequest,HttpResponse](s"${appConfig.developerBaseUrl}/developer/delete", deleteDeveloperRequest)
      .map(response => response.status match {
        case NO_CONTENT => DeveloperDeleteSuccessResult
        case _ => DeveloperDeleteFailureResult
      })
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      userId <- getOrCreateUser(email)
      user <- http.POST[RemoveMfaRequest, User](s"${appConfig.developerBaseUrl}/developer/${userId.value}/mfa/remove", RemoveMfaRequest(loggedInUser))
    } yield user
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
