package acceptance.mocks

import model.ApplicationState
import model.State
import org.joda.time.DateTime

trait ApplicationStateMock extends TestData {
  val testState: ApplicationState = ApplicationState(
     name = State.PRODUCTION,
     requestedByEmailAddress = Some(newAdminEmail),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )  

  val testStateForFetchAppResponseByEmail: ApplicationState = ApplicationState(
     name = State.PRODUCTION,
     requestedByEmailAddress = Some(newDeveloper),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kd w"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
  )  
}
