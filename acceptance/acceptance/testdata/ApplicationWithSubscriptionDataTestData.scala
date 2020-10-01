package acceptance.testdata

import model.applications.ApplicationWithSubscriptionData
import play.api.libs.json.Json

trait ApplicationWithSubscriptionDataTestData extends CommonTestData with SubscriptionsTestData with ApplicationTestData {
  val applicationWithSubscriptionData = ApplicationWithSubscriptionData(defaultApplication, defaultSubscriptions, Map.empty)

  val blockedApplicationWithSubscriptionData = ApplicationWithSubscriptionData(blockedApplication, defaultSubscriptions, Map.empty)

  val pendingApprovalApplicationWithSubscriptionData = ApplicationWithSubscriptionData(pendingApprovalApplication, defaultSubscriptions, Map.empty)

  implicit class ApplicationWithSubscriptionDataExtension(applicationWithSubscriptionData: ApplicationWithSubscriptionData) {
    import model.APIDefinitionFormatters._
    implicit val ApplicationWithSubscriptionDataFormat = Json.format[ApplicationWithSubscriptionData]

    def toJson = Json.toJson(applicationWithSubscriptionData)
    def toJsonString = Json.toJson(applicationWithSubscriptionData).toString
  }
}
