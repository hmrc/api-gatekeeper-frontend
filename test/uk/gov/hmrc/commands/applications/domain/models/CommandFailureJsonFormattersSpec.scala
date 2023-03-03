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

package uk.gov.hmrc.commands.applications.domain.models

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.{CommandFailure, CommandFailures}
import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec

class CommandFailureJsonFormattersSpec extends HmrcSpec {

  "CommandFailureJsonFormatters" should {
    import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailureJsonFormatters._

    "read a list of one failure from Json" in {
      val jsonText = """[{"failureType":"CannotRemoveLastAdmin"}]"""

      Json.parse(jsonText).as[List[CommandFailure]] shouldBe List(CommandFailures.CannotRemoveLastAdmin)
    }

    "read a list of failures from Json" in {
      val jsonText = """[{"failureType":"ApplicationNotFound"},{"failureType":"ActorIsNotACollaboratorOnApp"}]"""

      Json.parse(jsonText).as[List[CommandFailure]] shouldBe List(CommandFailures.ApplicationNotFound, CommandFailures.ActorIsNotACollaboratorOnApp)
    }
  }
}
