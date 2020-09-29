package acceptance.specs

import builder.{ApplicationBuilder, SubscriptionsBuilder, ApplicationResponseBuilder}
import model._
import model.applications.ApplicationWithSubscriptionData
import org.joda.time.DateTime
import play.api.libs.json.Json
import acceptance.mocks.{ApplicationStateMock, CollaboratorsMock, TestData}

trait NewApplicationTestData extends SubscriptionsBuilder with ApplicationBuilder with ApplicationResponseBuilder with CollaboratorsMock with ApplicationStateMock with TestData {
  val newApplicationName = "My new app"

  val testAccess: Access = Standard(
     redirectUris = Seq("http://localhost:8080/callback"),
     termsAndConditionsUrl = Some("http://localhost:22222/terms"),
     privacyPolicyUrl = Some("http://localhost:22222/privacy")
  )

  val testCheckInformation: CheckInformation = 
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

  val testIpWhitelist = Set.empty[String]

  val testApplication = DefaultApplication
   .withId(ApplicationId(newApplicationWithSubscriptionDataId))
   .withName(newApplicationName)
   .withDescription(newApplicationDescription)
   .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
   .withGatewayId("12345")
   .deployedToProduction
   .withCollaborators(collaborators)
   .withState(testState)
   .withAccess(testAccess)
   .withCheckInformation(testCheckInformation)
   .allowIPs(testIpWhitelist.toSeq :_*)
   .unblocked
   .withRateLimitTier(RateLimitTier.BRONZE)
   .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
   .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

  val testSubscriptions = Set(
     buildApiIdentifier(ApiContext("marriage-allowance"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("api-simulator"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("hello"), ApiVersion("1.0"))
  )

  val testApplicationWithSubscriptionData = ApplicationWithSubscriptionData(testApplication, testSubscriptions, Map.empty)

  import model.APIDefinitionFormatters._
  implicit val ApplicationWithSubscriptionDataFormat = Json.format[ApplicationWithSubscriptionData]

  val newApplicationWithSubscriptionData = Json.toJson(testApplicationWithSubscriptionData).toString

  val newStateHistoryTest = Seq(
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.TESTING, Actor(newAdminEmail), DateTime.parse("2019-08-22T11:21:50.160+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PENDING_GATEKEEPER_APPROVAL, Actor(newAdminEmail), DateTime.parse("2019-08-22T11:23:10.644+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PENDING_REQUESTER_VERIFICATION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T15:12:38.686+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PRODUCTION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T16:12:38.686+01:00"))
  )

  val newApplicationStateHistory = Json.toJson(newStateHistoryTest).toString

  val newApplicationUser = Json.toJson(unverifiedUser).toString

  val applicationResponseTest =    DefaultApplicationResponse
      .withId(ApplicationId(newApplicationWithSubscriptionDataId))
      .withName(newApplicationName)
      .withDescription("application for test")
      .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
      .withGatewayId("12345")
      .deployedToProduction
      .withCollaborators(collaboratorsForFetchAppResponseByEmail)
      .withState(testStateForFetchAppResponseByEmail)
      .withAccess(testAccess)
      .unblocked
      .withRateLimitTier(RateLimitTier.BRONZE)
      .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
      .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

   val applicationResponseForNewApplicationUserEmail = Json.toJson(Seq(applicationResponseTest)).toString

   val applicationResponseForNewApplicationTest = ApplicationWithHistory(applicationResponseTest, newStateHistoryTest)

    val applicationResponseForNewApplication = Json.toJson(applicationResponseForNewApplicationTest).toString
 }
