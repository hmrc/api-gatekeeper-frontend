package connectors

import utils.WiremockSpec
import model.ApplicationId
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.Application
import play.api.Mode
import play.api.Configuration
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.ws.WSClient

class ApmConnectorItSpec extends WiremockSpec with ApmConnectorMock {
  val fakeConfiguration = Configuration(
    "microservice.services.api-platform-microservice.host" -> "localhost",
    "microservice.services.api-platform-microservice.port" -> 11111
  )

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(fakeConfiguration)
      .in(Mode.Test)
      .build()

  trait Setup {
    val apmConnector = app.injector.instanceOf[ApmConnector]
    implicit val hc = new HeaderCarrier()
    val wsClient = app.injector.instanceOf[WSClient]
  }

  "Wiremock" should {
    val applicationId = ApplicationId.random
      
    "stub a GET request for fetchApplicationById" in new Setup {
      mockApplicationWithSubscriptionData(applicationId: ApplicationId)

      val response = await(apmConnector.fetchApplicationById(applicationId))

      response should not be None

      response.map { appWithSubsData =>
        appWithSubsData.application.id shouldBe applicationId
        appWithSubsData.subscriptionFieldValues shouldBe Map.empty
        appWithSubsData.application.collaborators.size shouldBe 1
      }
    }   
  }
}
