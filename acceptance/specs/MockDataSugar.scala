/*
 * Copyright 2020 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package specs

import org.scalacheck.Gen
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.models.UserId
import uk.gov.hmrc.gatekeeper.models.xml.{OrganisationId, VendorId, XmlApi, XmlOrganisation}

import java.util.UUID

object MockDataSugar {
  val approvedApp1 = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  val approvedApp2 = "a4b47c82-5888-41fd-aa83-da2bbd4679d1"
  val approvedApp3 = "9688ad02-230e-42b7-8f9a-be593565bfdc"
  val approvedApp4 = "56148b28-65b0-47dd-a3ce-2f02840ddd31"
  val appToDelete = "fa38d130-7c8e-47d8-abc0-0374c7f73216"

  val adminEmail = "admin@example.com"
  val adminId = UserId.random.value
  val admin2Email = "admin2@example.com"
  val admin2Id = UserId.random.value
  val firstName = "John"
  val lastName = "Test"

  val developer = "purnima.fakename@example.com"
  val devFirstName = "Purnima"
  val devLastName = "Fakename"
  val developerId = UserId.random.value

  val developer2 = "imran.fakename@example.com"
  val developer2Id = UserId.random.value
  val dev2FirstName = "Imran"
  val dev2LastName = "Fakename"

  val developer4 = "a.long.name.jane.hayjdjdu@a-very-long-email-address-exampleifi.com"
  val developer4Id = UserId.random.value
  val dev4FirstName = "HannahHmrcSdstusercollaboratir"
  val dev4LastName = "Kassidyhmrcdevusercollaborato"

  val developer5 = "John.fakename@example.com"
  val developer5Id = "1fe7b18e-a460-4936-b99f-fb0269b829e6"
  val dev5FirstName = "John"
  val dev5LastName = "Fakename"

  val developer6 = "Vijaya.fakename@example.com"
  val developer6Id = UserId.random.value
  val dev6FirstName = "Vijaya"
  val dev6LastName = "Fakename"

  val developer7 = "Kerri.fakename@example.com"
  val developer7Id = UserId.random.value
  val dev7FirstName = "Kerri"
  val dev7LastName = "Fakename"

  val developer8 = "Dixie.fakename@example.com"
  val developer8Id = UserId.random.value
  val dev8FirstName = "Dixie"
  val dev8LastName = "Fakename"

  val developer9 = "fred@example.com"
  val developer9Id = UserId.random.value
  val dev9name = "n/a"

  val developer10 = "peter.fakename@example.com"
  val developer10Id = UserId.random.value
  val dev10name = "n/a"

  val randomEmail = s"john.smith${System.currentTimeMillis}@example.com"

  val statusVerified = "verified"
  val statusUnverified = "not yet verified"
  val statusUnregistered = "not registered"

  val xmlApiOne = XmlApi(
    name = "xml api one",
    serviceName = "xml-api-one",
    context = "context",
    description = "description")

  val xmlApis = Json.toJson(Seq(xmlApiOne)).toString
  val orgOne = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()))
  val xmlOrganisations = Json.toJson(List(orgOne)).toString

  val approvedApplications =
    s"""
       |[
       |  {
       |    "id": "$approvedApp1",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Application",
       |    "submittedOn": 1458832690624,
       |    "state": "PENDING_REQUESTER_VERIFICATION"
       |  },
       |  {
       |    "id": "$approvedApp2",
       |    "clientId": "clientid2",
       |    "gatewayId": "gatewayId2",
       |    "name": "ZApplication",
       |    "submittedOn": 1458659208000,
       |    "state": "PRODUCTION"
       |  },
       |  {
       |    "id": "$approvedApp3",
       |    "clientId": "clientid3",
       |    "gatewayId": "gatewayId3",
       |    "name": "rapplication",
       |    "submittedOn": 1458831410657,
       |    "state": "PENDING_REQUESTER_VERIFICATION"
       |  },
       |  {
       |    "id": "$approvedApp4",
       |    "clientId": "clientid4",
       |    "gatewayId": "gatewayId4",
       |    "name": "BApplication",
       |    "submittedOn": 1458832728156,
       |    "state": "PRODUCTION"
       |  }
       |]
    """.stripMargin

  val applicationResponse =
    s"""
       |  [{
       |    "id": "$approvedApp1",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer",
       |      "userId": "$developerId",
       |      "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "emailAddress": "$developer9",
       |      "userId": "$developer9Id",
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
       |      "requestedByEmailAddress": "$developer",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [],
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  },
       |    {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "clientId": "clientId1",
       |    "gatewayId": "gatewayId2",
       |    "name": "Imrans Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer2",
       |      "userId": "$developer2Id",
       |      "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "emailAddress": "$developer7",
       |      "userId": "$developer7Id",
       |      "role": "DEVELOPER"
       |    },
       |    {
       |      "emailAddress": "$developer8",
       |      "userId": "$developer8Id",
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
       |      "requestedByEmailAddress": "$developer2",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [],
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  }]
    """.stripMargin

  val applicationResponseForEmail =
    s"""
       |  [{
       |    "id": "$appToDelete",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Automated Test Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer8",
       |      "userId": "$developer8Id",
       |     "role": "ADMINISTRATOR"
       |    },
       |    {
       |      "emailAddress": "$developer9",
       |      "userId": "$developer9Id",
       |     "role": "DEVELOPER"
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
       |      "requestedByEmailAddress": "$developer",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  }]
    """.stripMargin

  val applicationResponseWithNoUsers =
    s"""
       |  [{
       |    "id": "$approvedApp1",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |    "collaborators": [],
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
       |      "requestedByEmailAddress": "$developer",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [],
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  }]
    """.stripMargin

  val applicationResponsewithNoSubscription =
    s"""
       |  [{
       |    "id": "$approvedApp1",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |    "deployedTo": "PRODUCTION",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$developer4",
       |        "userId": "$developer4Id",
       |        "role": "ADMINISTRATOR"
       |      },
       |      {
       |        "emailAddress": "$developer5",
       |        "userId": "$developer5Id",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$developer6",
       |        "userId": "$developer6Id",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$developer10",
       |        "userId": "$developer10Id",
       |        "role": "DEVELOPER"
       |      }
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
       |      "requestedByEmailAddress": "$developer4",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [],
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  }]
    """.stripMargin

  val allUsers =
    s"""
       |[
       |  {
       |    "email": "$developer",
       |    "userId": "$developerId",
       |    "firstName": "$devFirstName",
       |    "lastName": "$devLastName",
       |    "verified": true,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer2",
       |    "userId": "$developer2Id",
       |    "firstName": "$dev2FirstName",
       |    "lastName": "$dev2LastName",
       |    "verified": true,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer4",
       |    "userId": "$developer4Id",
       |    "firstName": "$dev4FirstName",
       |    "lastName": "$dev4LastName",
       |    "verified": true,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer5",
       |    "userId": "$developer5Id",
       |    "firstName": "$dev5FirstName",
       |    "lastName": "$dev5LastName",
       |    "verified": false,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer6",
       |    "userId": "$developer6Id",
       |    "firstName": "$dev6FirstName",
       |    "lastName": "$dev6LastName",
       |    "verified": true,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer7",
       |    "userId": "$developer7Id",
       |    "firstName": "$dev7FirstName",
       |    "lastName": "$dev7LastName",
       |    "verified": true,
       |    "mfaEnabled": false,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  },
       |  {
       |    "email": "$developer8",
       |    "userId": "$developer8Id",
       |    "firstName": "$dev8FirstName",
       |    "lastName": "$dev8LastName",
       |    "verified": false,
       |    "mfaEnabled": true,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  }
       |]
   """.stripMargin

  val user =
    s"""
       |  {
       |    "email": "$developer8",
       |    "userId": "$developer8Id",
       |    "firstName": "$dev8FirstName",
       |    "lastName": "$dev8LastName",
       |    "verified": false,
       |    "mfaEnabled": true,
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  }
   """.stripMargin


  def approvedApplication(description: String = "", verified: Boolean = false) = {
    val verifiedHistory = if (verified) {
      s""",
         |    {
         |      "applicationId": "$approvedApp1",
         |      "clientId": "clientid1",
         |      "state": "PRODUCTION",
         |      "actor": {
         |        "id": "gatekeeper.username",
         |        "actorType": "GATEKEEPER"
         |      },
         |      "changedAt": 1459868522961
         |    }
      """.stripMargin
    } else {
      ""
    }

    val state = if (verified) {
      s"""
         |    "state": {
         |      "name": "PRODUCTION",
         |      "requestedByEmailAddress": "$adminEmail",
         |      "updatedOn": 1459868573962
         |    }
      """.stripMargin
    } else {
      s"""
         |    "state": {
         |      "name": "PENDING_REQUESTER_VERIFICATION",
         |      "requestedByEmailAddress": "$adminEmail",
         |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
         |      "updatedOn": 1459868573962
         |    }
      """.stripMargin
    }

    s"""
       |{
       |  "application": {
       |    "id": "$approvedApp1",
       |    "clientId": "clientid1",
       |    "gatewayId": "gatewayId1",
       |    "name": "Application",
       |    "description": "$description",
       |    "deployedTo": "PRODUCTION",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$adminEmail",
       |        "role": "ADMINISTRATOR"
       |      },
       |      {
       |        "emailAddress": "collaborator@example.com",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$admin2Email",
       |        "role": "ADMINISTRATOR"
       |      }
       |    ],
       |    "createdOn": 1459866628433,
       |    "lastAccess": 1459866628433,
       |    "redirectUris": [],
       |    "subscriptions": [],
       |    "access": {
       |      "redirectUris": [],
       |      "overrides": [],
       |      "accessType": "STANDARD"
       |    },
       |    "rateLimitTier": "BRONZE",
       |    $state,
       |    "ipAllowlist" : {
       |        "required" : false,
       |        "allowlist" : []
       |    },
       |    "grantLength": 547
       |  },
       |  "history": [
       |      {
       |      "applicationId": "$approvedApp1",
       |      "state": "PENDING_GATEKEEPER_APPROVAL",
       |      "actor": {
       |        "id": "$adminEmail",
       |        "actorType": "COLLABORATOR"
       |      },
       |      "changedAt": 1458659208000
       |    },
       |    {
       |      "applicationId": "$approvedApp1",
       |      "state": "PENDING_REQUESTER_VERIFICATION",
       |      "actor": {
       |        "id": "gatekeeper.username",
       |        "actorType": "GATEKEEPER"
       |      },
       |      "changedAt": 1459868522961
       |    }
       |    $verifiedHistory
       |  ]
       |}
    """.stripMargin
  }

  val StringGenerator = (n: Int) => Gen.listOfN(n, Gen.alphaChar).map(_.mkString)

  private val DeveloperGenerator: Gen[RegisteredUser] = for {
    forename <- StringGenerator(5)
    surname <- StringGenerator(5)
    id = UserId.random.value
    email = randomEmail
    verified = true
    registered = None
  } yield RegisteredUser(email, UserId(id), forename, surname, verified)

  def userListGenerator(number: Int): Gen[List[RegisteredUser]] = Gen.listOfN(number, DeveloperGenerator)

  def developerListJsonGenerator(number: Int): Option[String] =
    userListGenerator(number)
      .sample
      .map(_.sortWith((userA, userB) => userA.lastName > userB.lastName))
      .map(userList => Json.toJson(userList))
      .map(Json.stringify)


  def administrator(email: String = adminEmail, firstName: String = firstName, lastName: String = lastName) =
    s"""
       |{
       |"email": "$email",
       |"userId": "${UserId.random.value}",
       |"firstName": "$firstName",
       |"lastName": "$lastName",
       |"registrationTime": 1458300873012,
       |"lastModified": 1458300877382,
       |"verified": true,
       |"emailPreferences": {
       |   "interests": [],
       |    "topics": []
       | }
       |
       |}
     """.stripMargin


  val applicationSubscription =
    s"""
       [
       |  {
       |    "apiIdentifier": {
       |      "context": "individual-benefits",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "a97541e8-f93d-4d0a-ab0b-862e63204b7d",
       |      "4bf49df9-523a-4aa3-a446-683ff24b619f",
       |      "42695949-c7e8-4de9-a443-15c0da43143a"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "individual-employment",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "4bf49df9-523a-4aa3-a446-683ff24b619f",
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "95b381b8-499d-41e9-99b4-dbfed6a05752"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "individual-tax",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "individual-income",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "1abf06bf-45d7-47a9-aa1c-61fe4729f5b8"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "inheritance-tax",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "1abf06bf-45d7-47a9-aa1c-61fe4729f5b8",
       |      "79ad57d6-3691-45d5-b85d-6b8e0be8bcb1",
       |      "58dd6642-08c9-4422-8a84-058e8731d44a"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "national-insurance",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "1abf06bf-45d7-47a9-aa1c-61fe4729f5b8"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "national-insurance-record",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "1abf06bf-45d7-47a9-aa1c-61fe4729f5b8"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "marriage-allowance",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "42695949-c7e8-4de9-a443-15c0da43143a",
       |      "1abf06bf-45d7-47a9-aa1c-61fe4729f5b8",
       |      "ac1db09b-f8cf-440a-a3d2-86a81bc6b303"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "api-simulator",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "b69cfde4-3e62-48a3-93fd-cf5a1f5fd1be",
       |      "4fecb42e-15cb-4e91-8292-55ae406878e9",
       |      "58dd6642-08c9-4422-8a84-058e8731d44a",
       |      "af57d193-c9dd-4fdb-a790-656429c2f1dc",
       |      "ab349380-17cc-4de0-a7ac-c76baedd7133"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "employers-paye",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "e55def1d-763c-4a26-a44e-82a63f4cb70b",
       |      "10ff725b-9555-4919-a596-0850a3e83caa",
       |      "ac1db09b-f8cf-440a-a3d2-86a81bc6b303",
       |      "4afc248d-1c3e-4274-a77b-e89a25b4d764"
       |    ]
       |  },
       |  {
       |    "apiIdentifier": {
       |      "context": "self-assessment-api",
       |      "version": "1.0"
       |    },
       |    "applications": [
       |      "c9736f52-4202-4d14-85b5-cbd29601fa99",
       |      "10ff725b-9555-4919-a596-0850a3e83caa",
       |      "42695949-c7e8-4de9-a443-15c0da43143a"
       |    ]
       |  }
       |]
   """.stripMargin
}
