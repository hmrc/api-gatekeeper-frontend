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

object OverrideTypeHelper {

  implicit class DisplaySyntax(overrideType: OverrideType) {

    // TODO: Move to api-platform-application-domain?
    val displayText: String = overrideType match {
      case OverrideType.PERSIST_LOGIN_AFTER_GRANT      => "Persist login after grant"
      case OverrideType.GRANT_WITHOUT_TAXPAYER_CONSENT => "Grant without taxpayer consent"
      case OverrideType.SUPPRESS_IV_FOR_AGENTS         => "Suppress IV for agents"
      case OverrideType.SUPPRESS_IV_FOR_ORGANISATIONS  => "Suppress IV for organisations"
      case OverrideType.SUPPRESS_IV_FOR_INDIVIDUALS    => "Suppress IV for individuals"
      case _                                           => ""
    }
  }
}
