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

package model

import model.DeveloperStatusFilter.{AllStatus, DeveloperStatusFilter}

case class Developers2Filter(maybeEmailFilter: Option[String] = None,
                             maybeApiFilter: Option[ApiContextVersion] = None,
                             environmentFilter: ApiSubscriptionInEnvironmentFilter = AnyEnvironment,
                             developerStatusFilter: DeveloperStatusFilter = AllStatus)

case class ApiContextVersion(context: ApiContext, version: ApiVersion) {
  def toStringValue: String = s"${context.value}__$version"
}

object ApiContextVersion {
  private val ApiIdPattern = """^(.+)__(.+?)$""".r

  def apply(value: Option[String]): Option[ApiContextVersion] = {
    value match {
      case None => None
      case Some(ApiIdPattern(apiContext, version)) => Some(ApiContextVersion(ApiContext(apiContext), ApiVersion(version)))
      case _ => throw new Exception("Invalid API context or version")
    }
  }
}

case object DeveloperStatusFilter {

  sealed trait DeveloperStatusFilter {
    def isMatch(user: User): Boolean

    val value: String
  }

  case object VerifiedStatus extends DeveloperStatusFilter {
    val value = "VERIFIED"

    override def isMatch(user: User): Boolean =  user.verified match {
      case None => true
      case Some(verified) => verified
    }
  }

  case object UnverifiedStatus extends DeveloperStatusFilter {
    val value = "UNVERIFIED"

    override def isMatch(user: User): Boolean = !VerifiedStatus.isMatch(user)
  }

  case object AllStatus extends DeveloperStatusFilter {
    val value = "ALL"

    override def isMatch(user: User): Boolean = true
  }

  def apply(value: Option[String]): DeveloperStatusFilter = {
    value match {
      case Some(UnverifiedStatus.value) => UnverifiedStatus
      case Some(VerifiedStatus.value) => VerifiedStatus
      case Some(AllStatus.value) => AllStatus
      case None => AllStatus
      case Some(text) => throw new Exception("Invalid developer status filter: " + text)
    }
  }
}
