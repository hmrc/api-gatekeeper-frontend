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

import uk.gov.voa.play.form.ConditionalMappings.*

import play.api.data.*
import play.api.data.Forms.*

import uk.gov.hmrc.gatekeeper.models.UpliftAction

class HandleUpliftForm(val action: String, val reason: Option[String]) {}

object HandleUpliftForm {
  def apply(action: Option[String], reason: Option[String]): HandleUpliftForm = new HandleUpliftForm(action.getOrElse(""), reason)

  private def actionValidator =
    optional(text).verifying("invalid.action", action => UpliftAction.from(action.getOrElse("")).isDefined)

  lazy val form = Form(
    mapping(
      "action" -> actionValidator,
      "reason" -> mandatoryIfEqual("action", "REJECT", nonEmptyText)
    )(HandleUpliftForm.apply)(f => Some((Some(f.action), f.reason)))
  )
}

case class DevelopersSearchForm(
    maybeTextFilter: Option[String] = None,
    maybeApiVersionFilter: Option[String] = None,
    maybeEnvironmentFilter: Option[String] = None,
    maybeDeveloperStatusFilter: Option[String] = None
  )

object DevelopersSearchForm {

  lazy val form = Form(
    mapping(
      "textFilter"            -> optional(nonEmptyText),
      "apiVersionFilter"      -> optional(nonEmptyText),
      "environmentFilter"     -> optional(nonEmptyText),
      "developerStatusFilter" -> optional(nonEmptyText)
    )(DevelopersSearchForm.apply)(f => Some((f.maybeTextFilter, f.maybeApiVersionFilter, f.maybeEnvironmentFilter, f.maybeDeveloperStatusFilter)))
  )
}

case class UpdateRateLimitForm(tier: String)

object UpdateRateLimitForm {

  val form: Form[UpdateRateLimitForm] = Form(
    mapping(
      "tier" -> nonEmptyText
    )(UpdateRateLimitForm.apply)(f => Some((f.tier)))
  )
}

case class HandleApprovalForm(approval_confirmation: String)

object HandleApprovalForm {

  lazy val form = Form(
    mapping(
      "approval_confirmation" -> text(0, 20)
    )(HandleApprovalForm.apply)(f => Some((f.approval_confirmation)))
  )

  def unrecognisedAction(form: Form[HandleApprovalForm]) = {
    form
      .withError("submissionError", "true")
      .withGlobalError("Action is not recognised")
  }
}
