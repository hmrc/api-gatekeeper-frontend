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

package model

import play.api.data.Form
import play.api.data.Forms._
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfTrue

package object Forms {

  val loginForm = Form(
    mapping(
      "userName" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginDetails.make)(LoginDetails.unmake))

  val accessOverridesForm = Form (
    mapping (
      "persistLoginEnabled" -> boolean,
      "grantWithoutConsentEnabled" -> boolean,
      "grantWithoutConsentScopes" -> mandatoryIfTrue (
        "grantWithoutConsentEnabled",
        nonEmptyText
      ),
      "suppressIvForAgentsEnabled" -> boolean,
      "suppressIvForAgentsScopes" -> mandatoryIfTrue (
        "suppressIvForAgentsEnabled",
        nonEmptyText
      ),
      "suppressIvForOrganisationsEnabled" -> boolean,
      "suppressIvForOrganisationsScopes" -> mandatoryIfTrue (
        "suppressIvForOrganisationsEnabled",
        nonEmptyText
      )
    )((persistLoginEnabled: Boolean,
       grantWithoutConsentEnabled: Boolean,
       grantWithoutConsentScopes: Option[String],
       suppressIvForAgentsEnabled: Boolean,
       suppressIvForAgentsScopes: Option[String],
       suppressIvForOrganisationsEnabled: Boolean,
       suppressIvForOrganisationsScopes: Option[String]) => {
      val persistLogin = if(persistLoginEnabled) Some(PersistLogin()) else None
      val grantWithoutConsent = if(grantWithoutConsentEnabled) {
        Some(GrantWithoutConsent(grantWithoutConsentScopes.fold(Set.empty[String])(_.split(",").toSet)))
      } else {
        None
      }
      val suppressIvForAgents = if(suppressIvForAgentsEnabled) {
        Some(SuppressIvForAgents(suppressIvForAgentsScopes.fold(Set.empty[String])(_.split(",").toSet)))
      } else {
        None
      }
      val suppressIvForOrganisations = if(suppressIvForOrganisationsEnabled) {
        Some(SuppressIvForOrganisations(suppressIvForOrganisationsScopes.fold(Set.empty[String])(_.split(",").toSet)))
      } else {
        None
      }
      val overrides: Set[OverrideFlag] = Set(persistLogin, grantWithoutConsent, suppressIvForAgents, suppressIvForOrganisations).flatten
      overrides
    })((overrides: Set[OverrideFlag]) => {
      val persistLoginEnabled = overrides.exists(_.overrideType == OverrideType.PERSIST_LOGIN_AFTER_GRANT)
      val (grantWithoutConsentEnabled, grantWithoutConsentScopes) =
        overrides.find(_.overrideType == OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT) match {
          case Some(o: GrantWithoutConsent) => (true, Some(o.scopes.mkString(", ")))
          case _ => (false, None)
        }
      val (suppressIvForAgentsEnabled, suppressIvForAgentsScopes) =
        overrides.find(_.overrideType == OverrideType.SUPPRESS_IV_FOR_AGENTS) match {
          case Some(o: SuppressIvForAgents) => (true, Some(o.scopes.mkString(", ")))
          case _ => (false, None)
        }
      val (suppressIvForOrganisationsEnabled, suppressIvForOrganisationsScopes) =
        overrides.find(_.overrideType == OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS) match {
          case Some(o: SuppressIvForOrganisations) => (true, Some(o.scopes.mkString(", ")))
          case _ => (false, None)
        }

      Some(persistLoginEnabled,
        grantWithoutConsentEnabled,
        grantWithoutConsentScopes,
        suppressIvForAgentsEnabled,
        suppressIvForAgentsScopes,
        suppressIvForOrganisationsEnabled,
        suppressIvForOrganisationsScopes)
    }))
}
