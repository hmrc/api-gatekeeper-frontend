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

import uk.gov.voa.play.form.ConditionalMappings._

import play.api.data.Forms._
import play.api.data._

import uk.gov.hmrc.gatekeeper.models.UpliftAction

class HandleUpliftForm(val action: String, val reason: Option[String]) {}

object HandleUpliftForm {
  def apply(action: Option[String], reason: Option[String]): HandleUpliftForm = new HandleUpliftForm(action.getOrElse(""), reason)

  def unapply(form: HandleUpliftForm): Option[(Option[String], Option[String])] = Some((Some(form.action), form.reason))

  private def actionValidator =
    optional(text).verifying("invalid.action", action => UpliftAction.from(action.getOrElse("")).isDefined)

  lazy val form = Form(
    mapping(
      "action" -> actionValidator,
      "reason" -> mandatoryIfEqual("action", "REJECT", nonEmptyText)
    )(HandleUpliftForm.apply)(HandleUpliftForm.unapply)
  )
}

case class DevelopersSearchForm(
    maybeEmailFilter: Option[String] = None,
    maybeApiVersionFilter: Option[String] = None,
    maybeEnvironmentFilter: Option[String] = None,
    maybeDeveloperStatusFilter: Option[String] = None
  )

object DevelopersSearchForm {
  // def apply(e: Option[String], a: Option[String], env: Option[String], d: Option[String]): DevelopersSearchForm = new DevelopersSearchForm(e,a,env,d)

  // def unapply(form: DevelopersSearchForm): Option[(Option[String], Option[String], Option[String], Option[String])] = Some((form.maybeEmailFilter, form.maybeApiVersionFilter, form.maybeEnvironmentFilter, form.maybeDeveloperStatusFilter))

  lazy val form = Form(
    mapping(
      "emailFilter"           -> optional(nonEmptyText),
      "apiVersionFilter"      -> optional(nonEmptyText),
      "environmentFilter"     -> optional(nonEmptyText),
      "developerStatusFilter" -> optional(nonEmptyText)
    )(DevelopersSearchForm.apply)(DevelopersSearchForm.unapply)
  )
}

case class UpdateRateLimitForm(tier: String)

object UpdateRateLimitForm {

  val form: Form[UpdateRateLimitForm] = Form(
    mapping(
      "tier" -> nonEmptyText
    )(UpdateRateLimitForm.apply)(UpdateRateLimitForm.unapply)
  )
}

case class HandleApprovalForm(approval_confirmation: String)

object HandleApprovalForm {

  lazy val form = Form(
    mapping(
      "approval_confirmation" -> text(0, 20)
    )(HandleApprovalForm.apply)(HandleApprovalForm.unapply)
  )

  def unrecognisedAction(form: Form[HandleApprovalForm]) = {
    form
      .withError("submissionError", "true")
      .withGlobalError("Action is not recognised")
  }
}
