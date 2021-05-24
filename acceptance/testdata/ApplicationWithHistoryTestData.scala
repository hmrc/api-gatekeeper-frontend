package testdata

import model.ApplicationWithHistory

import play.api.libs.json.Json

trait ApplicationWithHistoryTestData extends ApplicationResponseTestData with StateHistoryTestData {

  val defaultApplicationWithHistory = ApplicationWithHistory(defaultApplicationResponse, stateHistories)

  val blockedApplicationWithHistory = ApplicationWithHistory(blockedApplicationResponse, stateHistories)

  val pendingApprovalApplicationWithHistory = ApplicationWithHistory(pendingApprovalApplicationResponse, pendingApprovalStateHistory)

  implicit class ApplicationWithHistoryExtension(applicationWithHistory: ApplicationWithHistory) {
    def toJson = Json.toJson(applicationWithHistory)
    def toJsonString = Json.stringify(Json.toJson(applicationWithHistory))
  }
}
