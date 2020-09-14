/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers

import play.api.mvc.PathBindable
import model.{ApiContext, ApplicationId, ApiVersion}
import play.api.mvc.QueryStringBindable

package object binders {
  implicit def applicationIdPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApplicationId] = new PathBindable[ApplicationId] {
    override def bind(key: String, value: String): Either[String, ApplicationId] = {
      textBinder.bind(key, value).map(ApplicationId(_))
    }

    override def unbind(key: String, applicationId: ApplicationId): String = {
      applicationId.value
    }
  }

  implicit def applicationIdQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApplicationId] {

    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApplicationId]] = {
      for {
        text <- textBinder.bind(key, params)
      } yield {
        text match {
          case Right(appId) => Right(ApplicationId(appId))
          case _            => Left("Unable to bind an application ID")
        }
      }
    }

    override def unbind(key: String, context: ApplicationId): String = {
      textBinder.unbind(key, context.value)
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

  implicit def apiVersionPathBinder(implicit textBinder: PathBindable[String]): PathBindable[ApiVersion] = new PathBindable[ApiVersion] {
    override def bind(key: String, value: String): Either[String, ApiVersion] = {
      textBinder.bind(key, value).map(ApiVersion(_))
    }

    override def unbind(key: String, apiVersion: ApiVersion): String = {
      apiVersion.value
    }
  }

  implicit def apiVersionQueryStringBindable(implicit textBinder: QueryStringBindable[String]) = new QueryStringBindable[ApiVersion] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ApiVersion]] = {
      for {
        version <- textBinder.bind("version", params)
      } yield {
        version match {
          case Right(version) => Right(ApiVersion(version))
          case _              => Left("Unable to bind an api version")
        }
      }
    }
    override def unbind(key: String, version: ApiVersion): String = {
      textBinder.unbind("version", version.value)
    }
  }
}
