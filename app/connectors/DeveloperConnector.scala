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

object DeveloperConnector extends DeveloperConnector {
  override val developerBaseUrl: String = s"${baseUrl("third-party-developer")}"
  override val http = WSHttp
}

trait DeveloperConnector {
  val developerBaseUrl: String
  val http: HttpPost with HttpGet

  def fetchByEmail(email: String)(implicit hc: HeaderCarrier): Future[User] = {
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

  def deleteDeveloper(deleteDeveloperRequest: DeleteDeveloperRequest)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult] = {
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
