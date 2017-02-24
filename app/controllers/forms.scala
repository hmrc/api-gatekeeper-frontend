/*
 * Copyright 2017 HM Revenue & Customs
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

import model.UpliftAction
import play.api.data._
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings._

case class HandleUpliftForm(action: String, reason: Option[String])

object HandleUpliftForm {

  private def actionValidator =
    Forms.text.verifying("invalid.action", action => UpliftAction.from(action).isDefined)

  lazy val form = Form(
    mapping(
      "action" -> actionValidator,
      "reason" -> mandatoryIfEqual("action","REJECT",nonEmptyText)
    )(HandleUpliftForm.apply)(HandleUpliftForm.unapply)
  )

  def unrecognisedAction(form: Form[HandleUpliftForm]) = {
    form
      .withError("submissionError", "true")
      .withGlobalError("Action is not recognised")
  }
}

case class HandleApprovalForm(approval_confirmation: String)

object HandleApprovalForm {

  lazy val form = Form(
    mapping(
      "approval_confirmation" -> text(0,20)
    )(HandleApprovalForm.apply)(HandleApprovalForm.unapply)
  )

  def unrecognisedAction(form: Form[HandleApprovalForm]) = {
    form
      .withError("submissionError", "true")
      .withGlobalError("Action is not recognised")
  }
}
