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

package uk.gov.hmrc.gatekeeper.controllers

import java.util.UUID
import scala.util.Try

import play.api.mvc.{PathBindable, QueryStringBindable}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models.{DeveloperIdentifier, EmailIdentifier}

package object binders extends ApplicationLogger {

  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    Try(UUID.fromString(text))
      .toOption
      .toRight(s"Cannot accept $text as ApplicationId")
      .map(uuid => ApplicationId(uuid))
  }

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).flatMap(applicationIdFromString)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value.toString()
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      textBinder.bind(key, params).map(_.flatMap(applicationIdFromString))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      textBinder.unbind(key, applicationId.value.toString())
    }
  }

  implicit def apiContextPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiContext] = new PathBindable[ApiContext] {

    override def bind(key: String, value: String): Either[String, ApiContext] = {
      textBinder.bind(key, value).map(ApiContext(_))
    }

    override def unbind(key: String, apiContext: ApiContext): String = {
      apiContext.value
    }
  }

  implicit def apiContextQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiContext] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiContext]] = {
      for {
        context <- textBinder.bind("context", params)
      } yield {
        context match {
          case Right(context) => Right(ApiContext(context))
          case _              => Left("Unable to bind an api context")
        }
      }
    }

    override def unbind(key: String, context: ApiContext): String = {
      textBinder.unbind("context", context.value)
    }
  }

  implicit def apiVersionPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiVersionNbr] = new PathBindable[ApiVersionNbr] {

    override def bind(key: String, value: String): Either[String, ApiVersionNbr] = {
      textBinder.bind(key, value).map(ApiVersionNbr(_))
    }

    override def unbind(key: String, apiVersion: ApiVersionNbr): String = {
      apiVersion.value
    }
  }

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiVersionNbr] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiVersionNbr]] = {
      for {
        version <- textBinder.bind("version", params)
      } yield {
        version match {
          case Right(version) => Right(ApiVersionNbr(version))
          case _              => Left("Unable to bind an api version")
        }
      }
    }

    override def unbind(key: String, versionNbr: ApiVersionNbr): String = {
      textBinder.unbind("version", versionNbr.value)
    }
  }

  import java.{util => ju}

  private def eitherFromString(text: String): Either[String, UserId] = {
    Try(ju.UUID.fromString(text))
      .toOption
      .toRight(s"Cannot accept $text as userId")
      .map(UserId(_))
  }

  implicit def userIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {

    override def bind(key: String, value: String): Either[String, UserId] = {
      textBinder.bind(key, value).flatMap(eitherFromString)
    }

    override def unbind(key: String, userId: UserId): String = {
      userId.value.toString()
    }
  }

  private def warnOnEmailId(source: String)(id: DeveloperIdentifier): DeveloperIdentifier = id match {
    case EmailIdentifier(_) => logger.warn(s"Still using emails as identifier - source:$source"); id
    case _                  => id
  }

  implicit def developerIdentifierBinder(implicit textBinder: PathBindable[String]): PathBindable[DeveloperIdentifier] = new PathBindable[DeveloperIdentifier] {

    override def bind(key: String, value: String): Either[String, DeveloperIdentifier] = {
      for {
        text <- textBinder.bind(key, value)
        id   <- DeveloperIdentifier(value).toRight(s"Cannot accept $text as a developer identifier")
        _     = warnOnEmailId(s"developerIdentifierBinder BIND $key")(id)
      } yield id
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      DeveloperIdentifier.asText(warnOnEmailId((s"developerIdentifierBinder UNBIND $key"))(developerId))
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[DeveloperIdentifier] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, DeveloperIdentifier]] = {
      for {
        textOrBindError <- textBinder.bind("developerId", params).orElse(textBinder.bind("email", params))
      } yield textOrBindError match {
        case Right(idText) =>
          for {
            id <- DeveloperIdentifier(idText).toRight(s"Cannot accept $idText as a developer identifier")
            _   = warnOnEmailId(s"queryStringBindable BIND $key")(id)
          } yield id
        case _             => Left("Unable to bind a developer identifier")
      }
    }

    override def unbind(key: String, developerId: DeveloperIdentifier): String = {
      textBinder.unbind("developerId", DeveloperIdentifier.asText(warnOnEmailId(s"queryStringBindable UNBIND $key")(developerId)))
    }
  }
}
