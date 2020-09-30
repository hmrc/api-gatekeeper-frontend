package acceptance.mocks

import model.StateHistory
import model.ApplicationId
import model.State
import model.Actor
import org.joda.time.DateTime
import play.api.libs.json.Json
import builder.StateHistoryBuilder

trait StateHistoryMock extends StateHistoryBuilder with TestData {
  val stateHistories = Seq(
     buildStateHistory(ApplicationId(applicationId), State.TESTING, Actor(administratorEmail), DateTime.parse("2019-08-22T11:21:50.160+01:00")),
     buildStateHistory(ApplicationId(applicationId), State.PENDING_GATEKEEPER_APPROVAL, Actor(administratorEmail), DateTime.parse("2019-08-22T11:23:10.644+01:00")),
     buildStateHistory(ApplicationId(applicationId), State.PENDING_REQUESTER_VERIFICATION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T15:12:38.686+01:00")),
     buildStateHistory(ApplicationId(applicationId), State.PRODUCTION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T16:12:38.686+01:00"))
  )

  val pendingApprovalStateHistory = Seq(
    buildStateHistory(ApplicationId(pendingApprovalApplicationId), State.PENDING_GATEKEEPER_APPROVAL, Actor(administratorEmail), DateTime.parse("2019-08-22T11:23:10.644+01:00")),
    buildStateHistory(ApplicationId(pendingApprovalApplicationId), State.PENDING_REQUESTER_VERIFICATION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T15:12:38.686+01:00"))
  )

  implicit class StateHistoryExtension(stateHistories: Seq[StateHistory]) {
    def toJson = Json.toJson(stateHistories)
    def toJsonString = Json.toJson(stateHistories).toString

    def withApplicationId(applicationId: ApplicationId) = stateHistories.map(sh => sh.copy(applicationId = applicationId))
  }
}
