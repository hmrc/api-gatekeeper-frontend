package acceptance.specs

trait NewApplicationMock {
  val newApplicationWithSubscriptionDataId = "a97541e8-f93d-4d0a-ab0b-862e63204b7d"
  val newApplicationDescription = "application description"
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
      |       "name": "My new app",
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
      |           "name": "PRODUCTION",
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
    |    "state": "TESTING",
    |    "actor": {
    |      "id": "$newAdminEmail"
    |    },
    |    "changedAt": "2019-08-22T11:21:50.160+01:00"
    |  },
    |  {
    |    "applicationId": "$newApplicationWithSubscriptionDataId",
    |    "state": "PENDING_GATEKEEPER_APPROVAL",
    |    "actor": {
    |      "id": "$newAdminEmail"
    |    },
    |    "changedAt": "2019-08-22T11:23:10.644+01:00"
    |  },
    |  {
    |    "applicationId": "$newApplicationWithSubscriptionDataId",
    |    "state": "PENDING_REQUESTER_VERIFICATION",
    |    "actor": {
    |      "id": "gatekeeper.username"
    |    },
    |    "changedAt": "2020-07-22T15:12:38.686+01:00"
    |  },
    |  {
    |    "applicationId": "$newApplicationWithSubscriptionDataId",
    |    "state": "PRODUCTION",
    |    "actor": {
    |      "id": "gatekeeper.username"
    |    },
    |    "changedAt": "2020-07-22T16:12:38.686+01:00"
    |  }
    |]""".stripMargin
}
