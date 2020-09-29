package acceptance.mocks

import model.User
import play.api.libs.json.Json

trait TestData {
  val newApplicationWithSubscriptionDataId = "a97541e8-f93d-4d0a-ab0b-862e63204b7d"
  val newApplicationDescription = "application description"
  val newApplicationName = "My new app"
  val newAdminEmail = "admin@example.com"

  val newDeveloper = "purnima.fakename@example.com"
  val newDeveloperFirstName = "Purnima"
  val newDeveloperLastName = "Fakename"

  val unverifiedUser = User(
    email = "Dixie.fakename@example.com",
    firstName = "Dixie",
    lastName = "Fakename",
    verified = Some(false),
    mfaEnabled = true
  )

  val newApplicationUser = Json.toJson(unverifiedUser).toString
}
