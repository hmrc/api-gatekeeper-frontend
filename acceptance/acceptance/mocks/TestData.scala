package acceptance.mocks

import model.User
import play.api.libs.json.Json

trait TestData {
  val applicationId = "a97541e8-f93d-4d0a-ab0b-862e63204b7d"
  val blockedApplicationId = "fa38d130-7c8e-47d8-abc0-0374c7f73217"
  val pendingApprovalApplicationId = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  
  val applicationDescription = "application description"
  val applicationName = "My new app"
  val blockedApplicationName = "Automated Test Application - Blocked"
  val pendingApprovalApplicationName = "Application requiring approval"

  val administratorEmail = "admin@example.com"

  val developerEmail = "purnima.fakename@example.com"
  val developerFirstName = "Purnima"
  val developerLastName = "Fakename"

  val unverifiedUser = User(
    email = "Dixie.fakename@example.com",
    firstName = "Dixie",
    lastName = "Fakename",
    verified = Some(false),
    mfaEnabled = true
  )

  val unverifiedUserJson = Json.toJson(unverifiedUser).toString
}
