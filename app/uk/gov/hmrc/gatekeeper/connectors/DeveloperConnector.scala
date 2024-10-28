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
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, _}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.tpd.core.dto.{FindOrCreateUserIdRequest, FindUserIdRequest, FindUserIdResponse, SearchParameters}
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.dto.RemoveAllMfaRequest
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.encryption._
import uk.gov.hmrc.gatekeeper.models.DeveloperStatusFilter.DeveloperStatusFilter
import uk.gov.hmrc.gatekeeper.models.{TopicOptionChoice, _}

@Singleton
class DeveloperConnector @Inject() (
    appConfig: AppConfig,
    http: HttpClientV2,
    @Named("ThirdPartyDeveloper") val payloadEncryption: PayloadEncryption
  )(implicit ec: ExecutionContext
  ) extends SendsSecretRequest with Logging {

  private def fetchUserId(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[CoreUserDetails]] = {
    http.post(url"${appConfig.developerBaseUrl}/developers/find-user-id")
      .withBody(Json.toJson(FindUserIdRequest(email)))
      .execute[Option[FindUserIdResponse]]
      .map(_.map(userIdResponse => CoreUserDetails(email, userIdResponse.userId)))
  }

  private def fetchOrCreateUserId(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[CoreUserDetails] = {
    http.post(url"${appConfig.developerBaseUrl}/developers/user-id")
      .withBody(Json.toJson(FindOrCreateUserIdRequest(email)))
      .execute[FindUserIdResponse]
      .map(userIdResponse => CoreUserDetails(email, userIdResponse.userId))
  }

  private def seekRegisteredUser(id: UserId)(implicit hc: HeaderCarrier): OptionT[Future, RegisteredUser] = {
    val queryParams = Seq("developerId" -> id.value.toString)
    OptionT(http.get(url"${appConfig.developerBaseUrl}/developer?$queryParams")
      .execute[Option[RegisteredUser]])
  }

  def seekUserByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[AbstractUser]] = {
    import cats.implicits._
    (
      for {
        coreUserDetails <- OptionT(fetchUserId(email))
        user            <- OptionT.liftF(seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id)))
      } yield user
    ).value
  }

  def fetchByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[AbstractUser] = {
    for {
      user <- seekRegisteredUser(userId).getOrElse(throw new IllegalArgumentException(s"$userId was not found, unexpectedly"))
    } yield user
  }

  def fetchByEmail(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[AbstractUser] = {
    for {
      optional       <- fetchUserId(email)
      coreUserDetails = optional.getOrElse(throw new IllegalArgumentException("Email was not found, unexpectedly"))
      user           <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchOrCreateUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[AbstractUser] = {
    for {
      coreUserDetails <- fetchOrCreateUserId(email)
      user            <- seekRegisteredUser(coreUserDetails.id).getOrElse(UnregisteredUser(email, coreUserDetails.id))
    } yield user
  }

  def fetchByEmails(emails: Iterable[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.post(url"${appConfig.developerBaseUrl}/developers/get-by-emails")
      .withBody(Json.toJson(emails))
      .execute[List[RegisteredUser]]
  }

  def fetchByEmailPreferences(
      topic: TopicOptionChoice,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategories: Option[Set[ApiCategory]] = None,
      privateapimatch: Boolean = false
    )(implicit hc: HeaderCarrier
    ): Future[List[RegisteredUser]] = {
    logger.info(s"fetchByEmailPreferences topic is $topic maybeApis: $maybeApis maybeApuCategories $maybeApiCategories privateapimatch $privateapimatch")
    val regimes: Seq[(String, String)] =
      maybeApiCategories.fold(Seq.empty[(String, String)])(regimes => regimes.toSeq.flatMap(regime => Seq("regime" -> regime.toString())))
    val privateapimatchParams          = if (privateapimatch) Seq("privateapimatch" -> "true") else Seq.empty
    val queryParams                    =
      Seq("topic" -> topic.toString) ++ regimes ++
        maybeApis.fold(Seq.empty[(String, String)])(apis => apis.map(("service" -> _))) ++ privateapimatchParams

    http.get(url"${appConfig.developerBaseUrl}/developers/email-preferences?$queryParams")
      .execute[List[RegisteredUser]]
  }

  def fetchByEmailPreferencesPaginated(
      maybeTopic: Option[TopicOptionChoice] = None,
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategories: Option[Set[ApiCategory]] = None,
      privateapimatch: Boolean = false,
      offset: Int,
      limit: Int
    )(implicit hc: HeaderCarrier
    ): Future[UserPaginatedResponse] = {
    logger.info(s"fetchByEmailPreferencesPaginated topic is $maybeTopic maybeApis: $maybeApis maybeApuCategories $maybeApiCategories privateapimatch $privateapimatch")
    val regimes: Seq[(String, String)] =
      maybeApiCategories.fold(Seq.empty[(String, String)])(regimes => regimes.toSeq.flatMap(regime => Seq("regime" -> regime.toString())))
    val apis                           = maybeApis.fold(Seq.empty[(String, String)])(apis => apis.map(("service" -> _)))
    val topic                          = Seq("topic" -> maybeTopic.map(_.toString).getOrElse(""))
    val privateApiMatchParams          = if (privateapimatch) Seq("privateapimatch" -> "true") else Seq.empty
    val pageParams                     = Seq("offset" -> s"$offset", "limit" -> s"$limit")
    val params                         = privateApiMatchParams ++ pageParams

    def prepareQueryParams = {
      (maybeTopic, maybeApis, maybeApiCategories) match {
        case (Some(_), None, None)       => topic ++ params
        case (None, Some(_), None)       => apis ++ params
        case (None, None, Some(_))       => regimes ++ params
        case (None, Some(_), Some(_))    => apis ++ regimes ++ params
        case (Some(_), None, Some(_))    => topic ++ regimes ++ params
        case (Some(_), Some(_), Some(_)) => topic ++ apis ++ regimes ++ params
        case (Some(_), Some(_), None)    => topic ++ apis ++ params
      }
    }

    http.get(url"${appConfig.developerBaseUrl}/developers/email-preferences-paginated?$prepareQueryParams")
      .execute[UserPaginatedResponse]
  }

  def fetchAll()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    http.get(url"${appConfig.developerBaseUrl}/developers/all").execute[List[RegisteredUser]]
  }

  def fetchAllPaginated(offset: Int, limit: Int)(implicit hc: HeaderCarrier): Future[UserPaginatedResponse] = {
    http.get(url"${appConfig.developerBaseUrl}/developers/all-paginated?offset=$offset&limit=$limit")
      .execute[UserPaginatedResponse]
  }

  def removeEmailPreferencesByService(serviceName: String)(implicit hc: HeaderCarrier): Future[EmailPreferencesDeleteResult] = {
    http.delete(url"${appConfig.developerBaseUrl}/developers/email-preferences/${serviceName}")
      .execute[HttpResponse]
      .map(response =>
        response.status match {
          case NO_CONTENT => EmailPreferencesDeleteSuccessResult
          case _          => EmailPreferencesDeleteFailureResult
        }
      )
      .recover {
        case _ => EmailPreferencesDeleteFailureResult
      }
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult] = {
    http.post(url"${appConfig.developerBaseUrl}/developer/delete")
      .withBody(Json.toJson(deleteDeveloperRequest))
      .execute[HttpResponse]
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

  def removeMfa(userId: UserId, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = {
    for {
      userResponse <- http.post(url"${appConfig.developerBaseUrl}/developer/$userId/mfa/remove").withBody(Json.toJson(RemoveAllMfaRequest(loggedInUser))).execute[RegisteredUser]
    } yield userResponse
  }

  def searchDevelopers(textSearch: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    val payload = SearchParameters(None, Some(status.value), textSearch)

    secretRequest(payload) { request =>
      http.post(url"${appConfig.developerBaseUrl}/developers/search").withBody(Json.toJson(request)).execute[List[RegisteredUser]]
    }
  }
}
