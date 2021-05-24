package testdata

import model._
import specs.MockDataSugar

import play.api.libs.json.Json

trait CommonTestData {
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

  val unverifiedUser = RegisteredUser(
    email = MockDataSugar.developer8,
    userId = UserId(MockDataSugar.developer8Id),
    firstName = MockDataSugar.dev8FirstName,
    lastName = MockDataSugar.dev8LastName,
    verified = false,
    mfaEnabled = true
  )
 

  val unverifiedUserJson = Json.toJson(unverifiedUser).toString

}
