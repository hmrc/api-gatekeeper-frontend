package acceptance.mocks

import model.ApplicationWithHistory
import play.api.libs.json.Json

trait ApplicationWithHistoryMock extends ApplicationResponseMock with StateHistoryMock {

  val applicationResponseForNewApplicationTest = ApplicationWithHistory(applicationResponseTest, stateHistories)

  val blockedApplicationResponseForNewApplicationTest = ApplicationWithHistory(blockedApplicationResponse, stateHistories)

  val pendingApprovalApplicationResponseForNewApplicationTest = ApplicationWithHistory(pendingApprovalApplicationResponse, pendingApprovalStateHistory)

  val applicationResponseForNewApplication = Json.toJson(applicationResponseForNewApplicationTest).toString

  implicit class ApplicationWithHistoryExtension(applicationWithHistory: ApplicationWithHistory) {
    def toJson = Json.toJson(applicationWithHistory)
    def toJsonString = Json.toJson(applicationWithHistory).toString
  }
}