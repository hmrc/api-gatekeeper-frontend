/*
 * Copyright 2018 HM Revenue & Customs
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

import model.Forms.FormFields._
import model.OverrideType._
import play.api.data.Form
import play.api.data.Forms._
import uk.gov.hmrc.emailaddress.EmailAddress
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

package object Forms {

  private val scopesRegex = """^[a-z:\-\,\s][^\r\n]+$""".r
  private def validScopes = text.verifying("override.scopes.incorrect", s => scopesRegex.findFirstIn(s).isDefined)

  object FormFields {
    val applicationNameConfirmation = "applicationNameConfirmation"
    val collaboratorEmail = "collaboratorEmail"
    val persistLoginEnabled = "persistLoginEnabled"
    val grantWithoutConsentEnabled = "grantWithoutConsentEnabled"
    val grantWithoutConsentScopes = "grantWithoutConsentScopes"
    val suppressIvForAgentsEnabled = "suppressIvForAgentsEnabled"
    val suppressIvForAgentsScopes = "suppressIvForAgentsScopes"
    val suppressIvForOrganisationsEnabled = "suppressIvForOrganisationsEnabled"
    val suppressIvForOrganisationsScopes = "suppressIvForOrganisationsScopes"
    val accessType = "accessType"
    val applicationName = "applicationName"
    val applicationDescription = "applicationDescription"
    val adminEmail = "adminEmail"
  }

  val loginForm = Form(
    mapping(
      "userName" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginDetails.make)(LoginDetails.unmake))

  val accessOverridesForm = Form (
    mapping (
      persistLoginEnabled -> boolean,
      grantWithoutConsentEnabled -> boolean,
      grantWithoutConsentScopes -> mandatoryIfTrue(grantWithoutConsentEnabled, validScopes),
      suppressIvForAgentsEnabled -> boolean,
      suppressIvForAgentsScopes -> mandatoryIfTrue(suppressIvForAgentsEnabled, validScopes),
      suppressIvForOrganisationsEnabled -> boolean,
      suppressIvForOrganisationsScopes -> mandatoryIfTrue(suppressIvForOrganisationsEnabled, validScopes)
    )(AccessOverridesForm.toSetOfOverrides)(AccessOverridesForm.fromSetOfOverrides))

  object AccessOverridesForm {
    def toSetOfOverrides(persistLoginEnabled: Boolean,
                         grantWithoutConsentEnabled: Boolean,
                         grantWithoutConsentScopes: Option[String],
                         suppressIvForAgentsEnabled: Boolean,
                         suppressIvForAgentsScopes: Option[String],
                         suppressIvForOrganisationsEnabled: Boolean,
                         suppressIvForOrganisationsScopes: Option[String]): Set[OverrideFlag] = {

      def overrideWithScopes(enabled: Boolean, scopes: Option[String], f: Set[String] => OverrideFlag): Option[OverrideFlag] = {
        if(enabled) Some(f(scopes.get.split(",").map(_.trim).toSet))
        else None
      }

      val persistLogin = if(persistLoginEnabled) Some(PersistLogin()) else None
      val grantWithoutConsent = overrideWithScopes(grantWithoutConsentEnabled, grantWithoutConsentScopes, GrantWithoutConsent)
      val suppressIvForAgents = overrideWithScopes(suppressIvForAgentsEnabled, suppressIvForAgentsScopes, SuppressIvForAgents)
      val suppressIvForOrganisations = overrideWithScopes(suppressIvForOrganisationsEnabled, suppressIvForOrganisationsScopes, SuppressIvForOrganisations)

      Set(persistLogin, grantWithoutConsent, suppressIvForAgents, suppressIvForOrganisations).flatten
    }

    def fromSetOfOverrides(overrides: Set[OverrideFlag]) = {

      def overrideWithScopes(overrides: Set[OverrideFlag], overrideType: OverrideType) = {
        overrides.find(_.overrideType == overrideType) match {
          case Some(o: OverrideFlagWithScopes) => (true, Some(o.scopes.mkString(", ")))
          case _ => (false, None)
        }
      }

      val persistLoginEnabled = overrides.exists(_.overrideType == PERSIST_LOGIN_AFTER_GRANT)

      val (grantWithoutConsentEnabled, grantWithoutConsentScopes) =
        overrideWithScopes(overrides, GRANT_WITHOUT_TAXPAYER_CONSENT)

      val (suppressIvForAgentsEnabled, suppressIvForAgentsScopes) =
        overrideWithScopes(overrides, SUPPRESS_IV_FOR_AGENTS)

      val (suppressIvForOrganisationsEnabled, suppressIvForOrganisationsScopes) =
        overrideWithScopes(overrides, SUPPRESS_IV_FOR_ORGANISATIONS)

      Some(
        persistLoginEnabled,
        grantWithoutConsentEnabled,
        grantWithoutConsentScopes,
        suppressIvForAgentsEnabled,
        suppressIvForAgentsScopes,
        suppressIvForOrganisationsEnabled,
        suppressIvForOrganisationsScopes
      )
    }
  }

  val scopesForm = Form (
    mapping (
      "scopes" -> validScopes
    )(ScopesForm.toSetOfScopes)(ScopesForm.fromSetOfScopes))

  val deleteApplicationForm = Form(
    mapping(
      applicationNameConfirmation -> text.verifying("application.confirmation.missing", _.nonEmpty),
      collaboratorEmail -> optional(email).verifying("application.administrator.missing", _.nonEmpty)
    )(DeleteApplicationForm.apply)(DeleteApplicationForm.unapply))

  object ScopesForm {
    def toSetOfScopes(scopes: String): Set[String] = scopes.split(",").map(_.trim).toSet

    def fromSetOfScopes(scopes: Set[String]) = Some(scopes.mkString(", "))
  }

  val createPrivOrROPCAppForm = Form(
    mapping(
      accessType -> optional(text).verifying("access.type.required", s => s.isDefined),
      applicationName -> text.verifying("application.name.required", _.nonEmpty),
      applicationDescription -> text.verifying("application.description.required", _.nonEmpty),
      adminEmail -> emailValidator
    )(CreatePrivOrROPCAppForm.apply)(CreatePrivOrROPCAppForm.unapply))

  private def emailValidator() = {
    play.api.data.Forms.text
      .verifying("email.required", _.length > 0)
      .verifying("email.not.valid", email => EmailAddress.isValid(email) || email.length == 0)
  }


}
