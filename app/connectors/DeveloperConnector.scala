/*
 * Copyright 2018 HM Revenue & Customs
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

import config.WSHttp
import connectors.AuthConnector._
import model._
import play.api.http.ContentTypes.JSON
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.Status.NO_CONTENT
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait DeveloperConnector {
  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User]
  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]]
  def fetchAll()(implicit hc: HeaderCarrier): Future[Seq[User]]
  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult]
}

trait HttpDeveloperConnector extends DeveloperConnector {
  val developerBaseUrl: String
  val http: HttpGet with HttpPost

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = {
    http.GET[User](s"$developerBaseUrl/developer", Seq("email" -> email)).recover{
      case e: NotFoundException => UnregisteredCollaborator(email)
    }
  }

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier) = {
    http.GET[Seq[User]](s"$developerBaseUrl/developers", Seq("emails" -> emails.mkString(",")))
  }

  def fetchAll()(implicit hc: HeaderCarrier) = {
    http.GET[Seq[User]](s"$developerBaseUrl/developers/all")
  }

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) = {
    http.POST(s"$developerBaseUrl/developer/delete", deleteDeveloperRequest, Seq(CONTENT_TYPE -> JSON))
      .map(response => response.status match {
        case NO_CONTENT => DeveloperDeleteSuccessResult
        case _ => DeveloperDeleteFailureResult
      })
      .recover {
        case _ => DeveloperDeleteFailureResult
      }
  }
}

object HttpDeveloperConnector extends HttpDeveloperConnector {
  override val developerBaseUrl = s"${baseUrl("third-party-developer")}"
  override val http = WSHttp
}

object DummyDeveloperConnector extends DeveloperConnector {
  def fetchByEmail(email: String)(implicit hc: HeaderCarrier) = Future.successful(UnregisteredCollaborator(email))

  def fetchByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def fetchAll()(implicit hc: HeaderCarrier) = Future.successful(Seq.empty)

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier) =
    Future.successful(DeveloperDeleteSuccessResult)
}
