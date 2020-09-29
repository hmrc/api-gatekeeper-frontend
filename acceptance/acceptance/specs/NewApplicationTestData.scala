package acceptance.specs

import builder.{ApplicationBuilder, SubscriptionsBuilder}
import model._
import model.applications.ApplicationWithSubscriptionData
import org.joda.time.DateTime
import play.api.libs.json.Json

trait NewApplicationTestData extends SubscriptionsBuilder with ApplicationBuilder {
  val newApplicationWithSubscriptionDataId = "a97541e8-f93d-4d0a-ab0b-862e63204b7d"
  val newApplicationDescription = "application description"
  val newAdminEmail = "admin@example.com"

  val newDeveloper = "purnima.fakename@example.com"
  val newDeveloperFirstName = "Purnima"
  val newDeveloperLastName = "Fakename"

  val newDeveloper8 = "Dixie.fakename@example.com"
  val newDeveloper8FirstName = "Dixie"
  val newDeveloper8LastName = "Fakename"

  val newApplicationName = "My new app"

  val testCollaborators: Set[Collaborator] = Set(
     Collaborator(newAdminEmail, CollaboratorRole.ADMINISTRATOR),
     Collaborator(newDeveloper, CollaboratorRole.DEVELOPER),
     Collaborator(newDeveloper8, CollaboratorRole.DEVELOPER)
  )

  val testAccess: Access = Standard(
     redirectUris = Seq("http://localhost:8080/callback"),
     termsAndConditionsUrl = Some("http://localhost:22222/terms"),
     privacyPolicyUrl = Some("http://localhost:22222/privacy")
  )

  val testState: ApplicationState = ApplicationState(
     name = State.PRODUCTION,
     requestedByEmailAddress = Some(newAdminEmail),
     verificationCode = Some("8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kdw"),
     updatedOn = DateTime.parse("2016-04-08T11:11:18.463Z")
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
   .withCollaborators(testCollaborators)
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

  val test = ApplicationWithSubscriptionData(testApplication, testSubscriptions, Map.empty)

  import model.APIDefinitionFormatters._
  implicit val ApplicationWithSubscriptionDataFormat = Json.format[ApplicationWithSubscriptionData]

  val newApplicationWithSubscriptionData = Json.toJson(test).toString

  val newStateHistoryTest = Seq(
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.TESTING, Actor(newAdminEmail), DateTime.parse("2019-08-22T11:21:50.160+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PENDING_GATEKEEPER_APPROVAL, Actor(newAdminEmail), DateTime.parse("2019-08-22T11:23:10.644+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PENDING_REQUESTER_VERIFICATION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T15:12:38.686+01:00")),
     buildStateHistory(ApplicationId(newApplicationWithSubscriptionDataId), State.PRODUCTION, Actor("gatekeeper.username"), DateTime.parse("2020-07-22T16:12:38.686+01:00"))
  )

  val newApplicationStateHistory = Json.toJson(newStateHistoryTest).toString

    val newApplicationUser =
    s"""
       |{
       |  "email": "$newDeveloper8",
       |  "firstName": "$newDeveloper8FirstName",
       |  "lastName": "$newDeveloper8LastName",
       |  "verified": false,
       |  "mfaEnabled": true
       |}
   """.stripMargin

    val applicationResponseForNewApplicationUserEmail =
    s"""
       |  [{
       |    "id": "$newApplicationWithSubscriptionDataId",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "$newApplicationName",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$newDeveloper8",
       |      "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "emailAddress": "fred@example.com",
       |      "role": "DEVELOPER"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "lastAccess": 1458832690624,
       |    "access": {
       |      "redirectUris": [],
       |      "overrides": [],
       |      "accessType": "STANDARD"
       |    },
       |    "rateLimitTier": "BRONZE",
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$newDeveloper",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    }
       |  }]
    """.stripMargin

    val applicationResponseForNewApplication =
    s"""
       |{
       |  "application": {
       |    "id": "$newApplicationWithSubscriptionDataId",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "$newApplicationName",
       |    "description": "$newApplicationDescription",
       |    "deployedTo": "PRODUCTION",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$newAdminEmail",
       |        "role": "ADMINISTRATOR"
       |      },
       |      {
       |        "emailAddress": "$newDeveloper",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$newDeveloper*",
       |        "role": "DEVELOPER"
       |      }
       |    ],
       |    "createdOn": 1459866628433,
       |    "lastAccess": 1459866628433,
       |    "redirectUris": [],
       |    "termsAndConditionsUrl": "http://www.example.com/termsAndConditions",
       |    "privacyPolicyUrl": "http://www.example.com/privacy",
       |    "access": {
       |      "redirectUris": [],
       |      "overrides": [],
       |      "termsAndConditionsUrl": "http://localhost:22222/terms",
       |      "privacyPolicyUrl": "http://localhost:22222/privacy",
       |      "accessType": "STANDARD"
       |    },
       |    "state": {
       |      "name": "PENDING_GATEKEEPER_APPROVAL",
       |      "requestedByEmailAddress": "$newAdminEmail",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "rateLimitTier": "BRONZE",
       |    "checkInformation": {
       |      "contactDetails": {
       |        "fullname": "Holly Golightly",
       |        "email": "holly.golightly@example.com",
       |        "telephoneNumber": "020 1122 3344"
       |      },
       |      "confirmedName": true,
       |      "providedPrivacyPolicyURL": true,
       |      "providedTermsAndConditionsURL": true,
       |      "applicationDetails": "An application that is pending approval",
       |      "termsOfUseAgreements": [{
       |        "emailAddress": "test@example.com",
       |        "timeStamp": 1459868573962,
       |        "version": "1.0"
       |      }]
       |    },
       |    "approvedDetails": {
       |      "details": {
       |        "id": "",
       |        "clientId": "",
       |        "name": "",
       |        "description": "",
       |        "rateLimitTier": "BRONZE",
       |        "submission": {
       |          "submitterName": "Barry Fakename",
       |          "submitterEmail": "barry.fakename@example.com",
       |          "submittedOn": 1459868573962
       |        },
       |        "reviewContactName": "Harry Golightly",
       |        "reviewContactEmail": "harry.fakename@example.com",
       |        "reviewContactTelephone": "020 1122 3345",
       |        "applicationDetails": ""
       |      },
       |      "admins": [],
       |      "approvedBy": "gatekeeperUserId",
       |      "approvedOn": 1459968573962,
       |      "verified": true
       |    },
       |    "blocked": false,
       |    "ipWhitelist": []
       |  },
       |  "history": [
       |      {
       |      "applicationId": "a6d37b4a-0a80-4b7f-b150-5f8f99fe27ea",
       |      "state": "PENDING_GATEKEEPER_APPROVAL",
       |      "actor": {
       |        "id": "$newAdminEmail",
       |        "actorType": "COLLABORATOR"
       |      },
       |      "changedAt": 1458659208000
       |    },
       |    {
       |      "applicationId": "a6d37b4a-0a80-4b7f-b150-5f8f99fe27ea",
       |      "state": "PENDING_REQUESTER_VERIFICATION",
       |      "actor": {
       |        "id": "gatekeeper.username",
       |        "actorType": "GATEKEEPER"
       |      },
       |      "changedAt": 1459868522961
       |    }
       |  ]
       |}
    """.stripMargin
}
