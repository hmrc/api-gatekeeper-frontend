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

package uk.gov.hmrc.apiplatform.modules.applications.access.domain.models

import play.api.libs.json.{JsSuccess, Json, OFormat, OWrites, Reads}
import uk.gov.hmrc.play.json.Union

sealed trait OverrideFlag {
  val overrideType: OverrideType
}

object OverrideFlag {
  implicit private val formatGrantWithoutConsent: OFormat[GrantWithoutConsent] = Json.format[GrantWithoutConsent]

  implicit private val formatPersistLogin: OFormat[PersistLogin.type] = OFormat[PersistLogin.type](
    Reads { _ => JsSuccess(PersistLogin) },
    OWrites[PersistLogin.type] { _ => Json.obj() }
  )

  implicit private val formatSuppressIvForAgents: OFormat[SuppressIvForAgents]               = Json.format[SuppressIvForAgents]
  implicit private val formatSuppressIvForOrganisations: OFormat[SuppressIvForOrganisations] = Json.format[SuppressIvForOrganisations]
  implicit private val formatSuppressIvForIndividuals: OFormat[SuppressIvForIndividuals]     = Json.format[SuppressIvForIndividuals]

  implicit val formatOverride = Union.from[OverrideFlag]("overrideType")
    .and[GrantWithoutConsent](OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT.toString)
    .and[PersistLogin.type](OverrideType.PERSIST_LOGIN_AFTER_GRANT.toString)
    .and[SuppressIvForAgents](OverrideType.SUPPRESS_IV_FOR_AGENTS.toString)
    .and[SuppressIvForOrganisations](OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS.toString)
    .and[SuppressIvForIndividuals](OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS.toString)
    .format
}

sealed trait OverrideFlagWithScopes extends OverrideFlag {
  val scopes: Set[String]
}

case object PersistLogin extends OverrideFlag {
  val overrideType = OverrideType.PERSIST_LOGIN_AFTER_GRANT
}

case class SuppressIvForAgents(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_AGENTS
}

case class SuppressIvForOrganisations(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS
}

case class SuppressIvForIndividuals(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS
}

case class GrantWithoutConsent(scopes: Set[String]) extends OverrideFlagWithScopes {
  val overrideType = OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT
}
