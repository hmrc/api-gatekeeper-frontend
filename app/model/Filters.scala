/*
 * Copyright 2016 HM Revenue & Customs
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


sealed trait ApiFilter[+A]
case class Value[A](a: A) extends ApiFilter[A]
case object NoApplications extends ApiFilter[Nothing]
case object NoSubscriptions extends ApiFilter[Nothing]
case object OneOrMoreSubscriptions extends ApiFilter[Nothing]
case object AllUsers extends ApiFilter[Nothing]

case object ApiFilter extends ApiFilter[String] {
  def apply(value: Option[String]): ApiFilter[String] = {
    value match {
      case Some("ALL") | Some("") | None => AllUsers
      case Some("ANYSUB") => OneOrMoreSubscriptions
      case Some("NOSUB") => NoSubscriptions
      case Some("NOAPP") => NoApplications
      case Some(flt) => Value(flt)
    }
  }
}

sealed trait StatusFilter
case object UnregisteredStatus extends StatusFilter
case object UnverifiedStatus extends StatusFilter
case object VerifiedStatus extends StatusFilter
case object AnyStatus extends StatusFilter

case object StatusFilter extends StatusFilter {
  def apply(value: Option[String]): StatusFilter = {
    value match {
      case Some("UNREGISTERED") => UnregisteredStatus
      case Some("UNVERIFIED") => UnverifiedStatus
      case Some("VERIFIED") => VerifiedStatus
      case _ => AnyStatus
    }
  }
}