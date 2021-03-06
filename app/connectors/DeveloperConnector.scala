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
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import model.UserId
import scala.concurrent.{ExecutionContext, Future}
import com.google.inject.name.Named
import uk.gov.hmrc.http.HttpReads.Implicits._
import cats.data.OptionT

trait DeveloperConnector {
  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def seekUserByEmail(email: String)(implicit hc: HeaderCarrier): Future[Option[User]]

  def fetchOrCreateUser(email: String)(implicit hc: HeaderCarrier): Future[User]

  final def fetchById(developerId: DeveloperIdentifier)(implicit hc: HeaderCarrier): Future[User] = developerId match {
    case EmailIdentifier(email) => fetchByEmail(email)
    case UuidIdentifier(userId) => fetchByUserId(userId)
  }

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategory: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult]

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser]
}

object DeveloperConnector {
  case class FindUserIdRequest(email: String)
  implicit val FindUserIdRequestWrite = Json.writes[FindUserIdRequest]

  case class FindUserIdResponse(userId: UserId)
  implicit val FindUserIdResponseReads = Json.reads[FindUserIdResponse]

  case class RemoveMfaRequest(removedBy: String)
  implicit val RemoveMfaRequestWrites = Json.writes[RemoveMfaRequest]

  case class GetOrCreateUserIdRequest(email: String)
  implicit val getOrCreateUserIdRequestFormat = Json.format[GetOrCreateUserIdRequest]
}

@Singleton
class HttpDeveloperConnector @Inject()(appConfig: AppConfig, http: HttpClient, @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption)
    (implicit ec: ExecutionContext) 
    extends DeveloperConnector
    with SendsSecretRequest {

  import DeveloperConnector._

  private def fetchUserId(email: String)(implicit hc: HeaderCarrier): Future[Option[CoreUserDetails]] = {
    http.POST[FindUserIdRequest, Option[FindUserIdResponse]](s"${appConfig.developerBaseUrl}/developers/find-user-id", FindUserIdRequest(email))
    .map(_.map(userIdResponse => CoreUserDetails(email, userIdResponse.userId)))
  }

  private def fetchOrCreateUserId(email: String)(implicit hc: HeaderCarrier): Future[CoreUserDetails] = {
    http.POST[GetOrCreateUserIdRequest, FindUserIdResponse](s"${appConfig.developerBaseUrl}/developers/user-id", GetOrCreateUserIdRequest(email))
    .map(userIdResponse => CoreUserDetails(email, userIdResponse.userId))
  }

  private def seekRegisteredUser(id: UserId)(implicit hc: HeaderCarrier): OptionT[Future,RegisteredUser] = 
    OptionT(http.GET[Option[RegisteredUser]](s"${appConfig.developerBaseUrl}/developer", Seq("developerId" -> id.value.toString)))
  
  def seekUserByEmail(email: String)(implicit hc: HeaderCarrier): Future[Option[User]] = {
    import cats.implicits._
    (
      for {
        coreUserDetails <- OptionT(fetchUserId(email))
        user            <- OptionT.liftF(seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id)))
      } yield user
    ).value
  }

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      user            <- seekRegisteredUser(userId).getOrElse(throw new IllegalArgumentException(s"${userId.value} was not found, unexpectedly"))
    } yield user
  }

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      optional        <- fetchUserId(email)
      coreUserDetails = optional.getOrElse(throw new IllegalArgumentException("Email was not found, unexpectedly"))
      user            <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchOrCreateUser(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      coreUserDetails <- fetchOrCreateUserId(email)
      user            <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.POST[Iterable[String], List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", emails)
  }

  def fetchByEmailPreferences(topic: TopicOptionChoice,
                              maybeApis: Option[Seq[String]] = None,
                              maybeApiCategories: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    val regimes: Seq[(String,String)] = maybeApiCategories.fold(Seq.empty[(String,String)])(regimes =>  regimes.flatMap(regime => Seq("regime" -> regime.value)))
    val queryParams =
      Seq("topic" -> topic.toString) ++ regimes ++
      maybeApis.fold(Seq.empty[(String,String)])(apis => apis.map(("service" -> _)))

    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/email-preferences", queryParams)
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/all")
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult] = {
    http.POST[DeleteDeveloperRequest,HttpResponse](s"${appConfig.developerBaseUrl}/developer/delete", deleteDeveloperRequest)
      .map(response => response.status match {
        case NO_CONTENT => DeveloperDeleteSuccessResult
        case _ => DeveloperDeleteFailureResult
      })
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = {
    for {
      user <- fetchById(developerId)
      userResponse <- http.POST[RemoveMfaRequest, RegisteredUser](s"${appConfig.developerBaseUrl}/developer/${user.userId.value}/mfa/remove", RemoveMfaRequest(loggedInUser))
    } yield userResponse
  }

  def searchDevelopers(maybeEmail: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    import model.SearchParameters

    val payload = SearchParameters(maybeEmail, Some(status.value))

    secretRequest(payload) { request =>
      http.POST[SecretRequest, List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/search", request)
    }
  }
}

@Singleton
class DummyDeveloperConnector extends DeveloperConnector {
  def seekUserByEmail(email: String)(implicit hc: HeaderCarrier): Future[Option[User]] = Future.successful(Some(UnregisteredUser(email, UserId.random)))

  def fetchOrCreateUser(email: String)(implicit hc: HeaderCarrier): Future[User] = Future.successful(UnregisteredUser(email, UserId.random))

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[User] = Future.successful(RegisteredUser("bob.smith@example.com", userId, "Bob", "Smith", true))

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = Future.successful(UnregisteredUser(email, UserId.random))

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier) = Future.successful(List.empty)

  def fetchAll()(implicit hc: HeaderCarrier) = Future.successful(List.empty)

  def fetchByEmailPreferences(topic: TopicOptionChoice, maybeApis: Option[Seq[String]] = None, maybeApiCategories: Option[Seq[APICategory]] = None)(implicit hc: HeaderCarrier) = Future.successful(List.empty)

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) =
    Future.successful(DeveloperDeleteSuccessResult)

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = Future.successful(RegisteredUser("bob.smith@example.com", UserId.random, "Bob", "Smith", true))

  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = Future.successful(List.empty)
}
