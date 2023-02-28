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

import cats.data.OptionT
import com.google.inject.name.Named

import play.api.Logging
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.encryption._
import uk.gov.hmrc.gatekeeper.models.DeveloperStatusFilter.DeveloperStatusFilter
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice.TopicOptionChoice
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress

trait DeveloperConnector {
  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def seekUserByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[User]]

  def fetchOrCreateUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[User]

  final def fetchById(developerId: DeveloperIdentifier)(implicit hc: HeaderCarrier): Future[User] = developerId match {
    case EmailIdentifier(email) => fetchByEmail(email)
    case UuidIdentifier(userId) => fetchByUserId(userId)
  }

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmails(emails: Iterable[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]]

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategory: Option[Seq[APICategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]]

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult]

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser]
}

object DeveloperConnector {
  case class FindUserIdRequest(email: LaxEmailAddress)
  implicit val FindUserIdRequestWrite = Json.writes[FindUserIdRequest]

  case class FindUserIdResponse(userId: UserId)
  implicit val FindUserIdResponseReads = Json.reads[FindUserIdResponse]

  case class RemoveMfaRequest(removedBy: String)
  implicit val RemoveMfaRequestWrites = Json.writes[RemoveMfaRequest]

  case class GetOrCreateUserIdRequest(email: LaxEmailAddress)
  implicit val getOrCreateUserIdRequestFormat = Json.format[GetOrCreateUserIdRequest]
}

@Singleton
class HttpDeveloperConnector @Inject() (
    appConfig: AppConfig,
    http: HttpClient,
    @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption
  )(implicit ec: ExecutionContext
  ) extends DeveloperConnector
    with SendsSecretRequest with Logging {

  import DeveloperConnector._

  private def fetchUserId(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[CoreUserDetails]] = {
    http.POST[FindUserIdRequest, Option[FindUserIdResponse]](s"${appConfig.developerBaseUrl}/developers/find-user-id", FindUserIdRequest(email))
      .map(_.map(userIdResponse => CoreUserDetails(email, userIdResponse.userId)))
  }

  private def fetchOrCreateUserId(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[CoreUserDetails] = {
    http.POST[GetOrCreateUserIdRequest, FindUserIdResponse](s"${appConfig.developerBaseUrl}/developers/user-id", GetOrCreateUserIdRequest(email))
      .map(userIdResponse => CoreUserDetails(email, userIdResponse.userId))
  }

  private def seekRegisteredUser(id: UserId)(implicit hc: HeaderCarrier): OptionT[Future, RegisteredUser] =
    OptionT(http.GET[Option[RegisteredUser]](s"${appConfig.developerBaseUrl}/developer", Seq("developerId" -> id.value.toString)))

  def seekUserByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[User]] = {
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
      user <- seekRegisteredUser(userId).getOrElse(throw new IllegalArgumentException(s"${userId.value} was not found, unexpectedly"))
    } yield user
  }

  def fetchByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      optional       <- fetchUserId(email)
      coreUserDetails = optional.getOrElse(throw new IllegalArgumentException("Email was not found, unexpectedly"))
      user           <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchOrCreateUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[User] = {
    for {
      coreUserDetails <- fetchOrCreateUserId(email)
      user            <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchByEmails(emails: Iterable[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.POST[Iterable[LaxEmailAddress], List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", emails)
  }

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategories: Option[Seq[APICategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    logger.info(s"fetchByEmailPreferences topic is $topic maybeApis: $maybeApis maybeApuCategories $maybeApiCategories privateapimatch $privateapimatch")
    val regimes: Seq[(String, String)] =
      maybeApiCategories.filter(_.nonEmpty).fold(Seq.empty[(String, String)])(regimes => regimes.filter(_.value.nonEmpty).flatMap(regime => Seq("regime" -> regime.value)))
    val privateapimatchParams          = if (privateapimatch) Seq("privateapimatch" -> "true") else Seq.empty
    val queryParams                    =
      Seq("topic" -> topic.toString) ++ regimes ++
        maybeApis.fold(Seq.empty[(String, String)])(apis => apis.map(("service" -> _))) ++ privateapimatchParams

    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/email-preferences", queryParams)
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.GET[List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/all")
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult] = {
    http.POST[DeleteDeveloperRequest, HttpResponse](s"${appConfig.developerBaseUrl}/developer/delete", deleteDeveloperRequest)
      .map(response =>
        response.status match {
          case NO_CONTENT => DeveloperDeleteSuccessResult
          case _          => DeveloperDeleteFailureResult
        }
      )
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = {
    for {
      user         <- fetchById(developerId)
      userResponse <- http.POST[RemoveMfaRequest, RegisteredUser](s"${appConfig.developerBaseUrl}/developer/${user.userId.value}/mfa/remove", RemoveMfaRequest(loggedInUser))
    } yield userResponse
  }

  def searchDevelopers(maybeEmail: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    import uk.gov.hmrc.gatekeeper.models.SearchParameters

    val payload = SearchParameters(maybeEmail, Some(status.value))

    secretRequest(payload) { request =>
      http.POST[SecretRequest, List[RegisteredUser]](s"${appConfig.developerBaseUrl}/developers/search", request)
    }
  }
}

@Singleton
class DummyDeveloperConnector extends DeveloperConnector {
  def seekUserByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[User]] = Future.successful(Some(UnregisteredUser(email, UserId.random)))

  def fetchOrCreateUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[User] = Future.successful(UnregisteredUser(email, UserId.random))

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[User] = Future.successful(RegisteredUser(LaxEmailAddress("bob.smith@example.com"), userId, "Bob", "Smith", true))

  def fetchByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier) = Future.successful(UnregisteredUser(email, UserId.random))

  def fetchByEmails(emails: Iterable[LaxEmailAddress])(implicit hc: HeaderCarrier) = Future.successful(List.empty)

  def fetchAll()(implicit hc: HeaderCarrier) = Future.successful(List.empty)

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategories: Option[Seq[APICategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ) = Future.successful(List.empty)

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) =
    Future.successful(DeveloperDeleteSuccessResult)

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] =
    Future.successful(RegisteredUser(LaxEmailAddress("bob.smith@example.com"), UserId.random, "Bob", "Smith", true))

  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = Future.successful(List.empty)
}
