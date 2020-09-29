package acceptance.mocks

import model.applications.ApplicationWithSubscriptionData
import play.api.libs.json.Json

trait ApplicationWithSubscriptionDataMock extends TestData with SubscriptionsMock with ApplicationMock {
  val newApplicationWithSubscriptionData = ApplicationWithSubscriptionData(testApplication, testSubscriptions, Map.empty)

  implicit class ApplicationWithSubscriptionDataExtension(applicationWithSubscriptionData: ApplicationWithSubscriptionData) {
    import model.APIDefinitionFormatters._
    implicit val ApplicationWithSubscriptionDataFormat = Json.format[ApplicationWithSubscriptionData]

    def toJson = Json.toJson(applicationWithSubscriptionData)
    def toJsonString = Json.toJson(applicationWithSubscriptionData).toString
  }
}