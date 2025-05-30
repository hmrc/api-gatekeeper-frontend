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

package uk.gov.hmrc.gatekeeper.models

import scala.util.{Failure, Try}

import org.apache.commons.net.util.SubnetUtils
import org.apache.commons.validator.routines.EmailValidator
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.validation._
import play.api.data.{Form, FormError}

import uk.gov.hmrc.apiplatform.modules.applications.access.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ValidatedApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.EmailOptionChoice._
import uk.gov.hmrc.gatekeeper.models.EmailPreferencesChoice._
import uk.gov.hmrc.gatekeeper.models.Forms.FormFields._

object Forms {

  private val scopesRegex = """^[a-z:\-\,\s][^\r\n]+$""".r

  private def validScopes = text.verifying("override.scopes.incorrect", s => scopesRegex.findFirstIn(s).isDefined)

  object FormFields {
    val applicationNameConfirmation       = "applicationNameConfirmation"
    val collaboratorEmail                 = "collaboratorEmail"
    val persistLoginEnabled               = "persistLoginEnabled"
    val grantWithoutConsentEnabled        = "grantWithoutConsentEnabled"
    val grantWithoutConsentScopes         = "grantWithoutConsentScopes"
    val originOverrideEnabled             = "originOverrideEnabled"
    val originOverrideValue               = "originOverrideValue"
    val suppressIvForAgentsEnabled        = "suppressIvForAgentsEnabled"
    val suppressIvForAgentsScopes         = "suppressIvForAgentsScopes"
    val suppressIvForOrganisationsEnabled = "suppressIvForOrganisationsEnabled"
    val suppressIvForOrganisationsScopes  = "suppressIvForOrganisationsScopes"
    val suppressIvForIndividualsEnabled   = "suppressIvForIndividualsEnabled"
    val suppressIvForIndividualsScopes    = "suppressIvForIndividualsScopes"
    val accessType                        = "accessType"
    val applicationName                   = "applicationName"
    val applicationDescription            = "applicationDescription"
    val adminEmail                        = "adminEmail"
    val environment                       = "environment"
    val sendEmailChoice                   = "sendEmailChoice"
    val sendEmailPreferences              = "sendEmailPreferences"
  }

  val accessOverridesForm = Form(
    mapping(
      persistLoginEnabled               -> boolean,
      originOverrideEnabled             -> boolean,
      originOverrideValue               -> mandatoryIfTrue(originOverrideEnabled, nonEmptyText),
      grantWithoutConsentEnabled        -> boolean,
      grantWithoutConsentScopes         -> mandatoryIfTrue(grantWithoutConsentEnabled, validScopes),
      suppressIvForAgentsEnabled        -> boolean,
      suppressIvForAgentsScopes         -> mandatoryIfTrue(suppressIvForAgentsEnabled, validScopes),
      suppressIvForOrganisationsEnabled -> boolean,
      suppressIvForOrganisationsScopes  -> mandatoryIfTrue(suppressIvForOrganisationsEnabled, validScopes),
      suppressIvForIndividualsEnabled   -> boolean,
      suppressIvForIndividualsScopes    -> mandatoryIfTrue(suppressIvForIndividualsEnabled, validScopes)
    )(AccessOverridesForm.toSetOfOverrides)(AccessOverridesForm.fromSetOfOverrides)
  )

  object AccessOverridesForm {

    def toSetOfOverrides(
        persistLoginEnabled: Boolean,
        originOverrideEnabled: Boolean,
        originOverrideValue: Option[String],
        grantWithoutConsentEnabled: Boolean,
        grantWithoutConsentScopes: Option[String],
        suppressIvForAgentsEnabled: Boolean,
        suppressIvForAgentsScopes: Option[String],
        suppressIvForOrganisationsEnabled: Boolean,
        suppressIvForOrganisationsScopes: Option[String],
        suppressIvForIndividualsEnabled: Boolean,
        suppressIvForIndividualsScopes: Option[String]
      ): Set[OverrideFlag] = {

      def overrideWithScopes(enabled: Boolean, scopes: Option[String], f: Set[String] => OverrideFlag): Option[OverrideFlag] = {
        if (enabled) Some(f(scopes.get.split(",").map(_.trim).toSet))
        else None
      }

      val persistLogin               = if (persistLoginEnabled) Some(OverrideFlag.PersistLogin) else None
      val originOverride             = if (originOverrideEnabled) Some(OverrideFlag.OriginOverride(originOverrideValue.get)) else None
      val grantWithoutConsent        = overrideWithScopes(grantWithoutConsentEnabled, grantWithoutConsentScopes, OverrideFlag.GrantWithoutConsent)
      val suppressIvForAgents        = overrideWithScopes(suppressIvForAgentsEnabled, suppressIvForAgentsScopes, OverrideFlag.SuppressIvForAgents)
      val suppressIvForOrganisations = overrideWithScopes(suppressIvForOrganisationsEnabled, suppressIvForOrganisationsScopes, OverrideFlag.SuppressIvForOrganisations)
      val suppressIvForIndividuals   = overrideWithScopes(suppressIvForIndividualsEnabled, suppressIvForIndividualsScopes, OverrideFlag.SuppressIvForIndividuals)

      Set(persistLogin, originOverride, grantWithoutConsent, suppressIvForAgents, suppressIvForOrganisations, suppressIvForIndividuals).flatten
    }

    def fromSetOfOverrides(overrides: Set[OverrideFlag]) = {

      def overrideWithScopes(overrides: Set[OverrideFlag], overrideType: OverrideType) = {
        overrides.find(_.overrideType == overrideType) match {
          case Some(o: OverrideFlag.SuppressIvForOrganisations) => (true, Some(o.scopes.mkString(", ")))
          case Some(o: OverrideFlag.SuppressIvForAgents)        => (true, Some(o.scopes.mkString(", ")))
          case Some(o: OverrideFlag.SuppressIvForIndividuals)   => (true, Some(o.scopes.mkString(", ")))
          case Some(o: OverrideFlag.GrantWithoutConsent)        => (true, Some(o.scopes.mkString(", ")))
          case Some(o: OverrideFlag.OriginOverride)             => (true, Some(o.origin))
          case _                                                => (false, None)
        }
      }

      val persistLoginEnabled = overrides.exists(_.overrideType == OverrideType.PERSIST_LOGIN_AFTER_GRANT)

      val (originOverrideEnabled, originOverrideValue) =
        overrideWithScopes(overrides, OverrideType.ORIGIN_OVERRIDE)

      val (grantWithoutConsentEnabled, grantWithoutConsentScopes) =
        overrideWithScopes(overrides, OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT)

      val (suppressIvForAgentsEnabled, suppressIvForAgentsScopes) =
        overrideWithScopes(overrides, OverrideType.SUPPRESS_IV_FOR_AGENTS)

      val (suppressIvForOrganisationsEnabled, suppressIvForOrganisationsScopes) =
        overrideWithScopes(overrides, OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS)

      val (suppressIvForIndividualsEnabled, suppressIvForIndividualsScopes) =
        overrideWithScopes(overrides, OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS)

      Some((
        persistLoginEnabled,
        originOverrideEnabled,
        originOverrideValue,
        grantWithoutConsentEnabled,
        grantWithoutConsentScopes,
        suppressIvForAgentsEnabled,
        suppressIvForAgentsScopes,
        suppressIvForOrganisationsEnabled,
        suppressIvForOrganisationsScopes,
        suppressIvForIndividualsEnabled,
        suppressIvForIndividualsScopes
      ))
    }
  }

  object ScopesForm {
    def toSetOfScopes(scopes: String): Set[String] = scopes.split(",").map(_.trim).toSet

    def fromSetOfScopes(scopes: Set[String]) = Some(scopes.mkString(", "))
  }

  val scopesForm: Form[Set[String]] = Form(
    mapping(
      "scopes" -> validScopes
    )(ScopesForm.toSetOfScopes)(ScopesForm.fromSetOfScopes)
  )

  def reduceValidationResults(a: ValidationResult, b: ValidationResult): ValidationResult = {
    (a, b) match {
      case (Valid, Valid)             => Valid
      case (Valid, i: Invalid)        => i
      case (i: Invalid, Valid)        => i
      case (Invalid(e1), Invalid(e2)) => Invalid(e1 ++ e2)
    }

  }
  case class IpAllowlistForm(required: Boolean, allowlist: Set[String])

  object IpAllowlistForm {

    private val privateNetworkRanges = Set(
      new SubnetUtils("10.0.0.0/8"),
      new SubnetUtils("172.16.0.0/12"),
      new SubnetUtils("192.168.0.0/16")
    ) map { su =>
      su.setInclusiveHostCount(true)
      su.getInfo
    }

    private val invalidNetworkRanges = Set(
      new SubnetUtils("127.0.0.1/32"),
      new SubnetUtils("255.255.255.255/32")
    ) map { sub =>
      sub.setInclusiveHostCount(true)
      sub.getInfo
    }

    val allowlistedIpsConstraint: Constraint[String] = Constraint({
      allowlistedIps => toSetOfAllowlistedIps(allowlistedIps).map(validateAllowlistedIp).fold(Valid)(reduceValidationResults)
    })

    def validateAllowlistedIp(allowlistedIp: String): ValidationResult = {

      Try(new SubnetUtils(allowlistedIp)) match {
        case Failure(_) => Invalid(Seq(ValidationError("ipAllowlist.invalid", allowlistedIp)))
        case _          =>
          val ipAndMask = allowlistedIp.split("/")
          if (privateNetworkRanges.exists(_.isInRange(ipAndMask(0)))) Invalid(Seq(ValidationError("ipAllowlist.invalid.private", allowlistedIp)))
          else if (invalidNetworkRanges.exists(_.isInRange(ipAndMask(0)))) Invalid(Seq(ValidationError("ipAllowlist.invalid.network", allowlistedIp)))
          else if (ipAndMask(1).toInt < 24) Invalid(Seq(ValidationError("ipAllowlist.invalid.range", allowlistedIp)))
          else Valid
      }
    }

    def toSetOfAllowlistedIps(allowlistedIps: String): Set[String] = allowlistedIps.split("""\s+""").map(_.trim).toSet.filterNot(_.isEmpty)

    def toForm(required: Boolean, allowlistedIps: String): IpAllowlistForm = IpAllowlistForm(required, toSetOfAllowlistedIps(allowlistedIps))

    def fromForm(form: IpAllowlistForm): Option[(Boolean, String)] = Some((form.required, form.allowlist.mkString("\n")))

    val form: Form[IpAllowlistForm] = Form(mapping(
      "required"       -> default(boolean, false),
      "allowlistedIps" -> text.verifying(allowlistedIpsConstraint)
    )(IpAllowlistForm.toForm)(IpAllowlistForm.fromForm))
  }

  val deleteApplicationForm = Form(
    mapping(
      applicationNameConfirmation -> text.verifying("application.confirmation.missing", _.nonEmpty),
      collaboratorEmail           -> optional(email).verifying("application.administrator.missing", _.nonEmpty)
    )(DeleteApplicationForm.apply)(DeleteApplicationForm.unapply)
  )

  val blockApplicationForm = Form(
    mapping(
      applicationNameConfirmation -> text.verifying("application.confirmation.missing", _.nonEmpty)
    )(BlockApplicationForm.apply)(BlockApplicationForm.unapply)
  )

  val unblockApplicationForm = Form(
    mapping(
      applicationNameConfirmation -> text.verifying("application.confirmation.missing", _.nonEmpty)
    )(UnblockApplicationForm.apply)(UnblockApplicationForm.unapply)
  )

  val createPrivAppForm = Form(
    mapping(
      environment            -> of[Environment],
      applicationName        -> text.verifying("application.name.required", s => ValidatedApplicationName.validate(s).isValid),
      applicationDescription -> text.verifying("application.description.required", _.nonEmpty),
      adminEmail             -> emailValidator()
    )(CreatePrivAppForm.apply)(CreatePrivAppForm.unapply)
  )

  implicit def environmentFormat: Formatter[Environment] = new Formatter[Environment] {

    override def bind(key: String, data: Map[String, String]) =
      data.get(key)
        .flatMap(name => Environment.apply(name))
        .toRight(Seq(FormError(key, "application.environment.required", Nil)))

    override def unbind(key: String, value: Environment) = Map(key -> value.toString)
  }

  private val defaultEmailValidator = EmailValidator.getInstance()

  private def emailValidator() = {
    text
      .verifying("email.required", _.nonEmpty)
      .verifying("email.not.valid", email => defaultEmailValidator.isValid(email) || email.isEmpty)
  }

  final case class AddTeamMemberForm(email: String, role: Option[String])

  object AddTeamMemberForm {

    def form: Form[AddTeamMemberForm] = Form(
      mapping(
        "email" -> emailValidator(),
        "role"  -> optional(text).verifying("team.member.error.role.invalid", _.isDefined)
      )(AddTeamMemberForm.apply)(AddTeamMemberForm.unapply)
    )
  }

  final case class RemoveTeamMemberForm(email: String)

  object RemoveTeamMemberForm {

    val form: Form[RemoveTeamMemberForm] = Form(
      mapping(
        "email" -> emailValidator()
      )(RemoveTeamMemberForm.apply)(RemoveTeamMemberForm.unapply)
    )
  }

  final case class RemoveTeamMemberConfirmationForm(email: String, confirm: Option[String] = Some(""))

  object RemoveTeamMemberConfirmationForm {

    val form: Form[RemoveTeamMemberConfirmationForm] = Form(
      mapping(
        "email"   -> emailValidator(),
        "confirm" -> optional(text).verifying("team.member.error.confirmation.no.choice.field", _.isDefined)
      )(RemoveTeamMemberConfirmationForm.apply)(RemoveTeamMemberConfirmationForm.unapply)
    )
  }

  object SendEmailChoiceForm {

    val form: Form[SendEmailChoice] = Form(
      mapping(
        sendEmailChoice -> of[EmailOptionChoice]
      )(SendEmailChoice.apply)(SendEmailChoice.unapply)
    )
  }

  implicit def emailOptionChoiceFormat: Formatter[EmailOptionChoice] = new Formatter[EmailOptionChoice] {

    override def bind(key: String, data: Map[String, String]) =
      data.get(key)
        .flatMap(name => Try(EmailOptionChoice.withName(name)).toOption)
        .toRight(Seq(FormError(key, "application.emailOption.required", Nil)))

    override def unbind(key: String, value: EmailOptionChoice) = Map(key -> value.toString)
  }

  object SendEmailPrefencesChoiceForm {

    val form: Form[SendEmailPreferencesChoice] = Form(
      mapping(
        sendEmailPreferences -> of[EmailPreferencesChoice]
      )(SendEmailPreferencesChoice.apply)(SendEmailPreferencesChoice.unapply)
    )
  }

  implicit def emailPreferencesChoiceFormat: Formatter[EmailPreferencesChoice] = new Formatter[EmailPreferencesChoice] {

    override def bind(key: String, data: Map[String, String]) =
      data.get(key)
        .flatMap(name => Try(EmailPreferencesChoice.withName(name)).toOption)
        .toRight(Seq(FormError(key, "application.emailPreferencesOption.required", Nil)))

    override def unbind(key: String, value: EmailPreferencesChoice) = Map(key -> value.toString)
  }

  final case class UpdateGrantLengthForm(grantLength: Option[Int])

  object UpdateGrantLengthForm {

    val form: Form[UpdateGrantLengthForm] = Form(
      mapping(
        "grantLength" -> optional(number).verifying("grantLength.required", _.isDefined)
      )(UpdateGrantLengthForm.apply)(UpdateGrantLengthForm.unapply)
    )
  }

  final case class DeleteRestrictionPreviouslyEnabledForm(confirm: String = "", reason: String = "", reasonDate: String = "")

  object DeleteRestrictionPreviouslyEnabledForm {

    val form: Form[DeleteRestrictionPreviouslyEnabledForm] = Form(
      mapping(
        "confirm"    -> text,
        "reason"     -> text,
        "reasonDate" -> text
      )(DeleteRestrictionPreviouslyEnabledForm.apply)(DeleteRestrictionPreviouslyEnabledForm.unapply)
        .verifying(
          "auto.delete.option.required",
          fields =>
            fields match {
              case data: DeleteRestrictionPreviouslyEnabledForm => data.confirm.nonEmpty
            }
        )
    )
  }

  final case class DeleteRestrictionPreviouslyDisabledForm(confirm: String = "", reason: String)

  object DeleteRestrictionPreviouslyDisabledForm {

    val form: Form[DeleteRestrictionPreviouslyDisabledForm] = Form(
      mapping(
        "confirm" -> text,
        "reason"  -> text
      )(DeleteRestrictionPreviouslyDisabledForm.apply)(DeleteRestrictionPreviouslyDisabledForm.unapply)
        .verifying(
          "auto.delete.reason.required",
          fields =>
            fields match {
              case data: DeleteRestrictionPreviouslyDisabledForm =>
                if (data.confirm.equalsIgnoreCase("no") && data.reason.isEmpty) false else true
            }
        )
    )
  }

  final case class UpdateApplicationNameForm(applicationName: String)

  object UpdateApplicationNameForm {

    val form: Form[UpdateApplicationNameForm] = Form(
      mapping(
        "applicationName" -> text.verifying("application.name.required", s => ValidatedApplicationName.validate(s).isValid)
      )(UpdateApplicationNameForm.apply)(UpdateApplicationNameForm.unapply)
    )
  }

  final case class UpdateApplicationNameAdminEmailForm(adminEmail: Option[String])

  object UpdateApplicationNameAdminEmailForm {

    val form: Form[UpdateApplicationNameAdminEmailForm] = Form(
      mapping(
        "adminEmail" -> optional(text).verifying("admin.email.required", _.isDefined)
      )(UpdateApplicationNameAdminEmailForm.apply)(UpdateApplicationNameAdminEmailForm.unapply)
    )
  }

  final case class RemoveMfaConfirmationForm(confirm: String = "")

  object RemoveMfaConfirmationForm {

    val form: Form[RemoveMfaConfirmationForm] = Form(
      mapping(
        "confirm" -> text
      )(RemoveMfaConfirmationForm.apply)(RemoveMfaConfirmationForm.unapply)
    )
  }

  final case class RemoveEmailPreferencesForm(serviceName: String = "")

  object RemoveEmailPreferencesForm {

    val form: Form[RemoveEmailPreferencesForm] = Form(
      mapping(
        "serviceName" -> text.verifying("serviceName.required", _.nonEmpty)
      )(RemoveEmailPreferencesForm.apply)(RemoveEmailPreferencesForm.unapply)
    )
  }
}
