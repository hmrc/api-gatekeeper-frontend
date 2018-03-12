package unit.utils

import java.util.UUID

import model._
import org.joda.time.DateTime
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec
import utils.ApplicationHelper

class ApplicationHelperSpec extends UnitSpec with Matchers {

  "ApplicationHelper" should {
    val adminEmail = "admin@example.com"
    val admin = Collaborator(adminEmail, CollaboratorRole.ADMINISTRATOR)
    val otherAdmin = Collaborator("otheradmin@example.com", CollaboratorRole.ADMINISTRATOR)
    val developer = Collaborator("someone@email.com", CollaboratorRole.DEVELOPER)

    val app1 = ApplicationResponse(UUID.randomUUID(), "application1", "PRODUCTION", None, Set(admin, otherAdmin, developer), DateTime.now(), Standard(), ApplicationState())
    val app2 = ApplicationResponse(UUID.randomUUID(), "application2", "PRODUCTION", None, Set(admin, otherAdmin), DateTime.now(), Standard(), ApplicationState())
    val app3 = ApplicationResponse(UUID.randomUUID(), "application3", "PRODUCTION", None, Set(admin, developer), DateTime.now(), Standard(), ApplicationState())
    val app4 = ApplicationResponse(UUID.randomUUID(), "application4", "PRODUCTION", None, Set(admin), DateTime.now(), Standard(), ApplicationState())

    "return a list of applications where the given email address is the only admin" in {
      val applications = Seq(app1, app2, app3, app4)
      ApplicationHelper.applicationsWithTeamMemberAsOnlyAdmin(applications, adminEmail) shouldBe Seq(app3, app4)
    }

    "return an empty list where no apps have the email address as the only admin" in {
      val applications = Seq(app1, app2)
      ApplicationHelper.applicationsWithTeamMemberAsOnlyAdmin(applications, adminEmail) shouldBe Seq.empty
    }
  }
}
