package acceptance.mocks

import model.ApplicationWithHistory
import play.api.libs.json.Json

trait ApplicationWithHistoryMock extends ApplicationResponseMock with StateHistoryMock {

  val defaultApplicationWithHistory = ApplicationWithHistory(defaultApplicationResponse, stateHistories)

  val blockedApplicationWithHistory = ApplicationWithHistory(blockedApplicationResponse, stateHistories)

  val pendingApprovalApplicationWithHistory = ApplicationWithHistory(pendingApprovalApplicationResponse, pendingApprovalStateHistory)

  implicit class ApplicationWithHistoryExtension(applicationWithHistory: ApplicationWithHistory) {
    def toJson = Json.toJson(applicationWithHistory)
    def toJsonString = Json.toJson(applicationWithHistory).toString
  }
}