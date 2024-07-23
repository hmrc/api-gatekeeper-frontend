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

import play.api.mvc.{PathBindable, QueryStringBindable}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice

package object binders extends ApplicationLogger {

  private def applicationIdFromString(text: String): Either[String, ApplicationId] = {
    ApplicationId.apply(text).toRight(s"Cannot accept $text as ApplicationId")
  }

  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {

    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).flatMap(applicationIdFromString)
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value.toString()
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApplicationId] = new QueryStringBindable[ApplicationId] {

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

  implicit def apiContextQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiContext] = new QueryStringBindable[ApiContext] {

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

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiVersionNbr] = new QueryStringBindable[ApiVersionNbr] {

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

  private def eitherFromString(text: String): Either[String, UserId] = {
    UserId.apply(text).toRight(s"Cannot accept $text as userId")
  }

  implicit def userIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {

    override def bind(key: String, value: String): Either[String, UserId] = {
      textBinder.bind(key, value).flatMap(eitherFromString)
    }

    override def unbind(key: String, userId: UserId): String = {
      userId.value.toString()
    }
  }

  // private def warnOnEmailId(source: String)(id: DeveloperIdentifier): DeveloperIdentifier = id match {
  //   case EmailIdentifier(_) => logger.warn(s"Still using emails as identifier - source:$source"); id
  //   case _                  => id
  // }

  implicit def developerIdentifierBinder(implicit textBinder: PathBindable[String]): PathBindable[UserId] = new PathBindable[UserId] {

    override def bind(key: String, value: String): Either[String, UserId] = {
      for {
        text <- textBinder.bind(key, value)
        id   <- UserId.apply(value).toRight(s"Cannot accept $text as a UserId")
      } yield id
    }

    override def unbind(key: String, userId: UserId): String = {
      textBinder.unbind("developerId", userId.value.toString)
    }
  }

  implicit def queryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[UserId] = new QueryStringBindable[UserId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, UserId]] = {
      for {
        textOrBindError <- textBinder.bind("developerId", params)
      } yield textOrBindError match {
        case Right(idText) =>
          for {
            id <- UserId.apply(idText).toRight(s"Cannot accept $idText as a userId")
          } yield id
        case _             => Left("Unable to bind a UserId")
      }
    }

    override def unbind(key: String, userId: UserId): String = {
      textBinder.unbind("developerId", userId.value.toString)
    }
  }

  private def apiCategoryFromString(text: String): Either[String, ApiCategory] = {
    ApiCategory(text).toRight(s"Cannot accept $text as ApiCategoryId")
  }

  implicit def apiCategoryPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiCategory] = new PathBindable[ApiCategory] {

    override def bind(key: String, value: String): Either[String, ApiCategory] = {
      textBinder.bind(key, value).flatMap(apiCategoryFromString)
    }

    override def unbind(key: String, apiCategory: ApiCategory): String = {
      apiCategory.toString()
    }
  }

  implicit def apiCategoryQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[ApiCategory] = new QueryStringBindable[ApiCategory] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiCategory]] = {
      textBinder.bind(key, params).map(_.flatMap(apiCategoryFromString))
    }

    override def unbind(key: String, apiCategory: ApiCategory): String = {
      textBinder.unbind(key, apiCategory.toString())
    }
  }

  private def topicFromString(text: String): Either[String, TopicOptionChoice] = {
    TopicOptionChoice(text).toRight(s"Cannot accept $text as ApiCategoryId")
  }

  implicit def topicQueryStringBindable(implicit textBinder: QueryStringBindable[String]): QueryStringBindable[TopicOptionChoice] = new QueryStringBindable[TopicOptionChoice] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, TopicOptionChoice]] = {
      textBinder.bind(key, params).map(_.flatMap(topicFromString))
    }

    override def unbind(key: String, topic: TopicOptionChoice): String = {
      textBinder.unbind(key, topic.toString())
    }
  }

  /** QueryString binder for Set
    */
  implicit def bindableSet[T: QueryStringBindable]: QueryStringBindable[Set[T]] =
    QueryStringBindable.bindableSeq[T].transform(_.toSet, _.toSeq)

}
