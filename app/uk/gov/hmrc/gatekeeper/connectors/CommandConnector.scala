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

import scala.concurrent.{ExecutionContext, Future}

import cats.data.NonEmptyList
import com.google.inject.{Inject, Singleton}

import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse, InternalServerException}

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{ApplicationCommand, CommandFailure, DispatchRequest, DispatchSuccessResult}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger

@Singleton
class CommandConnector @Inject()(http: HttpClient, config: ApmConnector.Config)(implicit val ec: ExecutionContext) extends ApplicationLogger {

  def dispatch(
      applicationId: ApplicationId,
      command: ApplicationCommand,
      adminsToEmail: Set[LaxEmailAddress]
    )(implicit hc: HeaderCarrier
    ): Future[Either[NonEmptyList[CommandFailure], DispatchSuccessResult]] = {

    import uk.gov.hmrc.apiplatform.modules.common.services.NonEmptyListFormatters._
    import play.api.libs.json._
    import uk.gov.hmrc.http.HttpReads.Implicits._
    import play.api.http.Status._

    def baseApplicationUrl(applicationId: ApplicationId) = s"${config.serviceBaseUrl}/applications/${applicationId.value.toString()}"

    def parseSuccessResponse(responseBody: String): DispatchSuccessResult =
      Json.parse(responseBody).validate[DispatchSuccessResult] match {
        case JsSuccess(result, _) => result
        case JsError(errs) => 
          logger.info(responseBody)
          logger.error(s"Failed parsing DispatchSuccessResult due to $errs")
          throw new InternalServerException("Failed parsing success response to dispatch")
      }

    def parseErrorResponse(responseBody: String): NonEmptyList[CommandFailure] =
      Json.parse(responseBody).validate[NonEmptyList[CommandFailure]] match {
        case JsSuccess(result, _) => result
        case JsError(errs) => 
          logger.info(responseBody)

          logger.error(s"Failed parsing NonEmptyList[CommandFailure] due to $errs")
          throw new InternalServerException("Failed parsing error response to dispatch")
      }

    val url          = s"${baseApplicationUrl(applicationId)}/dispatch"
    val request      = DispatchRequest(command, adminsToEmail)
    val extraHeaders = Seq.empty[(String, String)]
    import cats.syntax.either._

    http.PATCH[DispatchRequest, HttpResponse](url, request, extraHeaders)
      .map(_ match {
        case HttpResponse(BAD_REQUEST, body, _)             => parseErrorResponse(body).asLeft[DispatchSuccessResult]
        case HttpResponse(status, body, _) if status > 299  => logger.warn(s"Failed calling dispatch ($status) due to $body")
                                                               throw new InternalServerException("Failed calling dispatch")
        case HttpResponse(_, body, _)                       => parseSuccessResponse(body).asRight[NonEmptyList[CommandFailure]]
        }
      )
  }
}
