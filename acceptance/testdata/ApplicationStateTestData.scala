package testdata

import model.ApplicationState
import model.State
import org.joda.time.DateTime

trait ApplicationStateTestData extends CommonTestData {
  val productionState: ApplicationState = ApplicationState(
     name = State.PRODUCTION,
     requestedByEmailAddress = Some(administratorEmail),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )

  val pendingApprovalState: ApplicationState = ApplicationState(
     name = State.PENDING_GATEKEEPER_APPROVAL,
     requestedByEmailAddress = Some(administratorEmail),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )

  val stateForFetchAppResponseByEmail: ApplicationState = ApplicationState(
     name = State.PRODUCTION,
     requestedByEmailAddress = Some(developerEmail),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )  
}
