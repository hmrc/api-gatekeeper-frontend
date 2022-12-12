package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.models.ApplicationWithHistory
import play.api.libs.json.Json

trait ApplicationWithStateHistoryTestData extends ApplicationResponseTestData with StateHistoryTestData {

  val defaultApplicationWithHistory = ApplicationWithHistory(defaultApplicationResponse, stateHistories)

  val blockedApplicationWithHistory = ApplicationWithHistory(blockedApplicationResponse, stateHistories)

  val pendingApprovalApplicationWithHistory = ApplicationWithHistory(pendingApprovalApplicationResponse, pendingApprovalStateHistory)

  implicit class ApplicationWithHistoryExtension(applicationWithHistory: ApplicationWithHistory) {
    def toJson = Json.toJson(applicationWithHistory)
    def toJsonString = Json.stringify(Json.toJson(applicationWithHistory))
  }
}
