package uk.gov.hmrc.commands.applications.domain.models

import uk.gov.hmrc.apiplatform.modules.common.utils.HmrcSpec
import play.api.libs.json.Json
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailures
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models.CommandFailure

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
