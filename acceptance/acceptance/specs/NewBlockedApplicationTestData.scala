package acceptance.specs

trait NewBlockedApplicationTestData {
  val newBlockedApplicationWithSubscriptionDataId = "fa38d130-7c8e-47d8-abc0-0374c7f73217"
  val newBlockedApplicationDescription = "application description"
  val newBlockedAdminEmail = "admin@example.com"

  val newBlockedDeveloper = "purnima.fakename@example.com"
  val newBlockedDeveloperFirstName = "Purnima"
  val newBlockedDeveloperLastName = "Fakename"

  val newBlockedDeveloper8 = "Dixie.fakename@example.com"
  val newBlockedDeveloper8FirstName = "Dixie"
  val newBlockedDeveloper8LastName = "Fakename"

  val newBlockedApplicationName = "Automated Test Application - Blocked"

  val newBlockedApplicationWithSubscriptionData =
    s"""
      |{
      |   "application": {
      |       "id": "$newBlockedApplicationWithSubscriptionDataId",
      |       "clientId": "qDxLu6_zZVGurMX7NA7g2Wd5T5Ia",
      |       "blocked": true,
      |       "gatewayId": "12345",
      |       "rateLimitTier": "BRONZE",
      |       "name": "$newBlockedApplicationName",
      |       "createdOn": "2016-04-08T10:24:40.651Z",
      |       "lastAccess": "2019-07-01T00:00:00.000Z",
      |       "deployedTo": "PRODUCTION",
      |       "description": "$newBlockedApplicationDescription",
      |       "collaborators": [
      |           {
      |               "emailAddress": "$newBlockedAdminEmail",
      |               "role": "ADMINISTRATOR"
      |           },
      |           {
      |               "emailAddress": "$newBlockedDeveloper",
      |               "role": "DEVELOPER"
      |           },
      |           {
      |               "emailAddress": "$newBlockedDeveloper8",
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
      |           "name": "PRODUCTION",
      |           "requestedByEmailAddress": "$newBlockedAdminEmail",
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

  val newBlockedApplicationStateHistory = 
  s"""
    |[
    |  {
    |    "applicationId": "$newBlockedApplicationWithSubscriptionDataId",
    |    "state": "TESTING",
    |    "actor": {
    |      "id": "$newBlockedAdminEmail"
    |    },
    |    "changedAt": "2019-08-22T11:21:50.160+01:00"
    |  },
    |  {
    |    "applicationId": "$newBlockedApplicationWithSubscriptionDataId",
    |    "state": "PENDING_GATEKEEPER_APPROVAL",
    |    "actor": {
    |      "id": "$newBlockedAdminEmail"
    |    },
    |    "changedAt": "2019-08-22T11:23:10.644+01:00"
    |  },
    |  {
    |    "applicationId": "$newBlockedApplicationWithSubscriptionDataId",
    |    "state": "PENDING_REQUESTER_VERIFICATION",
    |    "actor": {
    |      "id": "gatekeeper.username"
    |    },
    |    "changedAt": "2020-07-22T15:12:38.686+01:00"
    |  },
    |  {
    |    "applicationId": "$newBlockedApplicationWithSubscriptionDataId",
    |    "state": "PRODUCTION",
    |    "actor": {
    |      "id": "gatekeeper.username"
    |    },
    |    "changedAt": "2020-07-22T16:12:38.686+01:00"
    |  }
    |]""".stripMargin

    val newBlockedApplicationUser =
    s"""
       |{
       |  "email": "$newBlockedDeveloper8",
       |  "firstName": "$newBlockedDeveloper8FirstName",
       |  "lastName": "$newBlockedDeveloper8LastName",
       |  "verified": false,
       |  "mfaEnabled": true
       |}
   """.stripMargin

    val blockedApplicationResponseForNewApplicationUserEmail =
    s"""
       |  [{
       |    "id": "$newBlockedApplicationWithSubscriptionDataId",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Automated Test Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$newBlockedDeveloper8",
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
       |      "requestedByEmailAddress": "$newBlockedDeveloper",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    }
       |  }]
    """.stripMargin

    val blockedApplicationResponseForNewApplication =
    s"""
       |{
       |  "application": {
       |    "id": "$newBlockedApplicationWithSubscriptionDataId",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "$newBlockedApplicationName",
       |    "description": "$newBlockedApplicationDescription",
       |    "deployedTo": "PRODUCTION",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$newBlockedAdminEmail",
       |        "role": "ADMINISTRATOR"
       |      },
       |      {
       |        "emailAddress": "$newBlockedDeveloper",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$newBlockedDeveloper*",
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
       |      "requestedByEmailAddress": "$newBlockedAdminEmail",
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
       |        "id": "$newBlockedAdminEmail",
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
