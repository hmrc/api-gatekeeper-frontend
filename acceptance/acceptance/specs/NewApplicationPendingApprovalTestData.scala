package acceptance.specs

trait NewApplicationPendingApprovalTestData {
  val newApplicationWithSubscriptionDataId = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  val newApplicationDescription = "application description"
  val newApplicationName = "Application requiring approval"
  val newAdminEmail = "admin@example.com"

  val newDeveloper = "purnima.fakename@example.com"
  val newDeveloperFirstName = "Purnima"
  val newDeveloperLastName = "Fakename"

  val newDeveloper8 = "Dixie.fakename@example.com"
  val newDeveloper8FirstName = "Dixie"
  val newDeveloper8LastName = "Fakename"

  val newApplicationWithSubscriptionData =
    s"""
      |{
      |   "application": {
      |       "id": "$newApplicationWithSubscriptionDataId",
      |       "clientId": "qDxLu6_zZVGurMX7NA7g2Wd5T5Ia",
      |       "blocked": false,
      |       "gatewayId": "12345",
      |       "rateLimitTier": "BRONZE",
      |       "name": "$newApplicationName",
      |       "createdOn": "2016-04-08T10:24:40.651Z",
      |       "lastAccess": "2019-07-01T00:00:00.000Z",
      |       "deployedTo": "PRODUCTION",
      |       "description": "$newApplicationDescription",
      |       "collaborators": [
      |           {
      |               "emailAddress": "$newAdminEmail",
      |               "role": "ADMINISTRATOR"
      |           },
      |           {
      |               "emailAddress": "$newDeveloper",
      |               "role": "DEVELOPER"
      |           },
      |           {
      |               "emailAddress": "$newDeveloper8",
      |               "role": "DEVELOPER"
      |           }
      |       ],
      |       "access": {
      |       "redirectUris": [
      |           "http://localhost:8080/callback"
      |       ],
      |       "termsAndConditionsUrl": "http://localhost:22222/terms",
      |       "privacyPolicyUrl": "http://localhost:22222/privacy",
      |       "overrides": [],
      |       "accessType": "STANDARD"
      |       },
      |       "state": {
      |           "name": "PENDING_GATEKEEPER_APPROVAL",
      |           "requestedByEmailAddress": "$newAdminEmail",
      |           "verificationCode": "8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kdw",
      |           "updatedOn": "2016-04-08T11:11:18.463Z"
      |       },
      |       "checkInformation": {
      |         "contactDetails": {
      |           "fullname": "Holly Golightly",
      |           "email": "holly.golightly@example.com",
      |           "telephoneNumber": "020 1122 3344"
      |         },
      |         "confirmedName": true,
      |         "providedPrivacyPolicyURL": true,
      |         "providedTermsAndConditionsURL": true,
      |         "applicationDetails": "",
      |         "termsOfUseAgreements": [{
      |           "emailAddress": "test@example.com",
      |           "timeStamp": 1459868573962,
      |           "version": "1.0"
      |         }]
      |       },
      |       "ipWhitelist": []
      |   },
      |   "subscriptions": [
      |       {
      |       "context": "marriage-allowance",
      |       "version": "1.0"
      |       },
      |       {
      |       "context": "api-simulator",
      |       "version": "1.0"
      |       },
      |       {
      |       "context": "hello",
      |       "version": "1.0"
      |       }
      |   ],
      |   "subscriptionFieldValues": {}
      |}""".stripMargin

  val newApplicationStateHistory = 
  s"""
    |[
    |  {
    |    "applicationId": "$newApplicationWithSubscriptionDataId",
    |    "state": "PENDING_GATEKEEPER_APPROVAL",
    |    "actor": {
    |      "id": "$newAdminEmail"
    |    },
    |    "changedAt": "2019-08-22T11:23:10.644+01:00"
    |  }
    |]""".stripMargin

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
