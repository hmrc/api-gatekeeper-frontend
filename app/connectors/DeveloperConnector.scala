/*
 * Copyright 2019 HM Revenue & Customs
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
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

trait DeveloperConnector {
  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]]

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User]

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]]

  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[User]]

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult]

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User]
}

@Singleton
class HttpDeveloperConnector @Inject()(appConfig: AppConfig, http: HttpClient)(implicit ec: ExecutionContext) extends DeveloperConnector {

  private val postHeaders: Seq[(String, String)] = Seq(CONTENT_TYPE -> JSON)

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = {
    http.GET[User](s"${appConfig.developerBaseUrl}/developer", Seq("email" -> email)).recover {
      case e: NotFoundException => UnregisteredCollaborator(email)
    }
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    http.POST[JsValue, Seq[User]](s"${appConfig.developerBaseUrl}/developers/get-by-emails", Json.toJson(emails), postHeaders)
  }

  def fetchAll()(implicit hc: HeaderCarrier) = {
    http.GET[Seq[User]](s"${appConfig.developerBaseUrl}/developers/all")
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) = {
    http.POST(s"${appConfig.developerBaseUrl}/developer/delete", deleteDeveloperRequest, postHeaders)
      .map(response => response.status match {
        case NO_CONTENT => DeveloperDeleteSuccessResult
        case _ => DeveloperDeleteFailureResult
      })
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = {
    http.POST[JsValue, User](s"${appConfig.developerBaseUrl}/developer/$email/mfa/remove", Json.obj("removedBy" -> loggedInUser))
  }

  def searchDevelopers(maybeEmail: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]] = {

    val queryParams = (maybeEmail, status) match {
      case (None, status) => Seq("status" -> status.value)
      case (Some(email), status) => Seq("emailFilter" -> email, "status" -> status.value)
    }

    http.GET[Seq[User]](s"${appConfig.developerBaseUrl}/developers", queryParams)
  }
}

@Singleton
class DummyDeveloperConnector @Inject()(implicit ec: ExecutionContext) extends DeveloperConnector {
  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = Future.successful(UnregisteredCollaborator(email))

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def fetchAll()(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) =
    Future.successful(DeveloperDeleteSuccessResult)

  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = Future.successful(UnregisteredCollaborator(email))

  def searchDevelopers(email: Option[String], status: DeveloperStatusFilter)(implicit hc: HeaderCarrier): Future[Seq[User]] = Future.successful(Seq.empty)
}
