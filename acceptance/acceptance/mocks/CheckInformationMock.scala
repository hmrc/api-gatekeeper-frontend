package acceptance.mocks

import model.{CheckInformation, ContactDetails, TermsOfUseAgreement}
import org.joda.time.DateTime

trait CheckInformationMock {
  val defaultCheckInformation: CheckInformation =
    CheckInformation(
      contactDetails = Some(
         ContactDetails(
            fullname = "Holly Golightly",
            email = "holly.golightly@example.com",
            telephoneNumber = "020 1122 3344"
         )
      ),
      confirmedName = true,
      providedPrivacyPolicyURL = true,
      providedTermsAndConditionsURL = true,
      applicationDetails = Some(""),
      termsOfUseAgreements = Seq(
         TermsOfUseAgreement(
            emailAddress = "test@example.com",
            timeStamp = new DateTime(1459868573962L),
            version = "1.0"
         )
      )
    )
}
