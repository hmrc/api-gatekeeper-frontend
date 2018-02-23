package unit.view.applications

import java.util.UUID

import config.AppConfig
import model._
import org.joda.time.DateTime
import org.scalatestplus.play.{OneServerPerSuite, PlaySpec}
import play.api.mvc.Flash
import play.api.i18n.Messages.Implicits._
import play.api.test.FakeRequest

class ApplicationViewSpec extends PlaySpec with OneServerPerSuite {
  "application view" must {
    "show application information, including Delete Application button, when logged in as superuser" in {
      implicit val request = FakeRequest()
      val application =
        ApplicationResponse(
          UUID.randomUUID(),
          "application1",
          None,
          Set(Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR), Collaborator("someone@email.com", CollaboratorRole.DEVELOPER)),
          DateTime.now(),
          Standard(),
          ApplicationState()
        )
      val applicationWithHistory = ApplicationWithHistory(application, Seq.empty)

      val result = views.html.applications.application.render(applicationWithHistory, Seq.empty, true, request, None, Flash.emptyCookie, applicationMessages, AppConfig)

      result.contentType must include("text/html")
      result.body must contain("Delete Application")
    }
  }
}
