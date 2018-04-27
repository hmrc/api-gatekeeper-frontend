package unit.model

import java.util.UUID

import model.CollaboratorRole._
import model.SubscribedApplicationResponse._
import model._
import org.joda.time.DateTime
import org.scalatest.Matchers
import uk.gov.hmrc.play.test.UnitSpec

class SubscribedApplicationResponseSpec extends UnitSpec with Matchers {

  def randomText = UUID.randomUUID().toString

  "SubscribedApplicationResponse" should {
    "create from ApplicationResponse" in {
      val id = UUID.randomUUID()
      val name = randomText
      val description = randomText
      val collaborator = Collaborator(randomText, DEVELOPER)
      val createdOn = DateTime.now
      val state = ApplicationState()
      val sub = SubscriptionNameAndVersion("subName", "subVersion")
      val termsOfUseAccepted = false

      val appResponse = ApplicationResponse(id, name, randomText, Some(description), Set(collaborator), createdOn, Standard(), state)

      val expected = SubscribedApplicationResponse(id, name, Some(description), Set(collaborator), createdOn, state, Seq(sub),
        termsOfUseAccepted)
      createFrom(appResponse, Seq()) shouldBe expected
    }
    "identify terms of use accepted" in {

    }
  }
}