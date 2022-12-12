/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.models

sealed trait ApiFilter[+A]
case class Value[A](context: A, version: A) extends ApiFilter[A]
case object NoApplications                  extends ApiFilter[Nothing]
case object OneOrMoreApplications           extends ApiFilter[Nothing]
case object NoSubscriptions                 extends ApiFilter[Nothing]
case object OneOrMoreSubscriptions          extends ApiFilter[Nothing]
case object AllUsers                        extends ApiFilter[Nothing]

case object ApiFilter {
  private val ApiIdPattern = """^(.+)__(.+?)$""".r

  def apply(value: Option[String]): ApiFilter[String] = {
    value match {
      case Some("ALL") | Some("") | None        => AllUsers
      case Some("ANYSUB")                       => OneOrMoreSubscriptions
      case Some("ANYAPP")                       => OneOrMoreApplications
      case Some("NOSUB")                        => NoSubscriptions
      case Some("NOAPP")                        => NoApplications
      case Some(ApiIdPattern(context, version)) => Value(context, version)
    }
  }
}

sealed trait StatusFilter
case object UnregisteredStatus extends StatusFilter
case object UnverifiedStatus   extends StatusFilter
case object VerifiedStatus     extends StatusFilter
case object AnyStatus          extends StatusFilter

case object StatusFilter {

  def apply(value: Option[String]): StatusFilter = {
    value match {
      case Some("UNREGISTERED") => UnregisteredStatus
      case Some("UNVERIFIED")   => UnverifiedStatus
      case Some("VERIFIED")     => VerifiedStatus
      case _                    => AnyStatus
    }
  }
}

sealed trait ApiSubscriptionInEnvironmentFilter
case object AnyEnvironment        extends ApiSubscriptionInEnvironmentFilter
case object ProductionEnvironment extends ApiSubscriptionInEnvironmentFilter
case object SandboxEnvironment    extends ApiSubscriptionInEnvironmentFilter

case object ApiSubscriptionInEnvironmentFilter {

  def apply(value: Option[String]): ApiSubscriptionInEnvironmentFilter = value match {
    case Some("PRODUCTION") => ProductionEnvironment
    case Some("SANDBOX")    => SandboxEnvironment
    case _                  => AnyEnvironment
  }
}
