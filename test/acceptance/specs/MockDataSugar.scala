/*
 * Copyright 2016 HM Revenue & Customs
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

package acceptance.specs

import model.User
import org.scalacheck.Gen
import play.api.libs.json.Json

trait MockDataSugar {
  val appPendingApprovalId1 = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  val appPendingApprovalId2 = "a4b47c82-5888-41fd-aa83-da2bbd4679d1"
  val approvedApp1 = "df0c32b6-bbb7-46eb-ba50-e6e5459162ff"
  val approvedApp2 = "a4b47c82-5888-41fd-aa83-da2bbd4679d1"
  val approvedApp3 = "9688ad02-230e-42b7-8f9a-be593565bfdc"
  val approvedApp4 = "56148b28-65b0-47dd-a3ce-2f02840ddd31"

  val applicationDescription = "application description"
  val adminEmail = "admin@test.com"
  val admin2Email = "admin2@test.com"
  val firstName = "John"
  val lastName = "Test"
  val fullName = s"$firstName $lastName"

  val developer = "purnima.shanti@mail.com"
  val devFirstName = "Purnima"
  val devLastName = "Shanti"

  val developer2 = "imran.akram@mail.com"
  val dev2FirstName = "Imran"
  val dev2LastName = "Akram"

  val developer3 = "gurpreet.bhamra@mail.com"
  val dev3FirstName = "Gurpreet"
  val dev3LastName =  "Bhamra"

  val developer4 = "a.long.name.jane.hayjdjdu@a-very-long-email-address-exampleifi.com"
  val dev4FirstName = "HannahHmrcSdstusercollaboratir"
  val dev4LastName = "Kassidyhmrcdevusercollaborato"

  val developer5 = "John.Dave@mail.com"
  val dev5FirstName = "John"
  val dev5LastName =  "Dave"

  val developer6 = "Vijaya.Vasantha@mail.com"
  val dev6FirstName = "Vijaya"
  val dev6LastName =  "Vasantha"

  val developer7 = "Kerri Ruffo"
  val dev7FirstName = "Kerri"
  val dev7LastName = "Ruffo"

  val developer8 = "Dixie Upton"
  val dev8FirstName = "Dixie"
  val dev8LastName = "Upton"

  val developer9 = "Marvella Shumpert"
  val dev9FirstName = "Marvella"
  val dev9LastName = "Shumpert"

  val developer10 = "Glen Zaleski"
  val dev10FirstName = "Glen"
  val dev10LastName = "Zaleski"

  val developer11 = "Toni Gillins"
  val dev11FirstName = "Toni"
  val dev11LastName = "Gillins"

  val randomEmail = s"john.smith${System.currentTimeMillis}@mail.com"

  val statusVerified = "verified"
  val statusUnverified = "not yet verified"

  val applicationsPendingApproval =
    s"""
       |[
       |  {
       |    "id": "$appPendingApprovalId2",
       |    "name": "Second Application",
       |    "submittedOn": 1458832690624,
       |    "state": "PENDING_GATEKEEPER_APPROVAL"
       |  },
       |  {
       |    "id": "$appPendingApprovalId1",
       |    "name": "First Application",
       |    "submittedOn": 1458659208000,
       |    "state": "PENDING_GATEKEEPER_APPROVAL"
       |  },
       |  {
       |    "id": "9688ad02-230e-42b7-8f9a-be593565bfdc",
       |    "name": "Third",
       |    "submittedOn": 1458831410657,
       |    "state": "PENDING_REQUESTER_VERIFICATION"
       |  },
       |  {
       |    "id": "56148b28-65b0-47dd-a3ce-2f02840ddd31",
       |    "name": "Fourth",
       |    "submittedOn": 1458832728156,
       |    "state": "PRODUCTION"
       |  }
       |]
    """.stripMargin

  val approvedApplications =
    s"""
       |[
       |  {
       |    "id": "$approvedApp1",
       |    "name": "Application",
       |    "submittedOn": 1458832690624,
       |    "state": "PENDING_REQUESTER_VERIFICATION"
       |  },
       |  {
       |    "id": "$approvedApp2",
       |    "name": "ZApplication",
       |    "submittedOn": 1458659208000,
       |    "state": "PRODUCTION"
       |  },
       |  {
       |    "id": "$approvedApp3",
       |    "name": "RApplication",
       |    "submittedOn": 1458831410657,
       |    "state": "PENDING_REQUESTER_VERIFICATION"
       |  },
       |  {
       |    "id": "$approvedApp4",
       |    "name": "BApplication",
       |    "submittedOn": 1458832728156,
       |    "state": "PRODUCTION"
       |  }
       |]
    """.stripMargin

  val application =
    s"""
       |{
       |  "application": {
       |    "id": "$appPendingApprovalId1",
       |    "name": "First Application",
       |    "description": "$applicationDescription",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$adminEmail",
       |        "role": "ADMINISTRATOR"
       |      }
       |    ],
       |    "createdOn": 1459866628433,
       |    "redirectUris": [],
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$adminEmail",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |  "history": [
       |      {
       |      "applicationId": "a6d37b4a-0a80-4b7f-b150-5f8f99fe27ea",
       |      "state": "PENDING_GATEKEEPER_APPROVAL",
       |      "actor": {
       |        "id": "$adminEmail",
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

  val applicationResponse =
    s"""
       |  [{
       |    "id": "$approvedApp1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [
       |    {
       |     "context": "individual-paye",
       |     "version": "1.0"
       |     }
       |     ]
       |  },
       |    {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "name": "Imrans Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer2",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer2",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |    {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "name": "Gurpreets Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer3",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer3",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |      {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "name": "Gurpreets Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer4",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer4",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |      {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "name": "Gurpreets Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer5",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer5",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |    {
       |    "id": "df0c32b6-bbb7-46eb-ba50-e6e5459162ff",
       |    "name": "Long Persons Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer6",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer6",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  },
       |    {
       |    "id": "$approvedApp1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$randomEmail",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$randomEmail",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [
       |    {
       |     "context": "any-api",
       |     "version": "1.0"
       |     }
       |     ]
       |  }]
    """.stripMargin

  val applicationResponsewithAPI =
    s"""
       |  [{
       |    "id": "$approvedApp1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer5",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer5",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": [
       |    {
       |     "context": "individual-paye",
       |     "version": "1.0"
       |     }
       |     ]
       |  },
       |    {
       |    "id": "$approvedApp1",
       |    "name": "Purnimas Application",
       |    "description": "application for test",
       |   "collaborators": [
       |    {
       |      "emailAddress": "$developer6",
       |     "role": "ADMINISTRATOR"
       |    }
       |    ],
       |    "createdOn": 1458832690624,
       |    "state": {
       |      "name": "PRODUCTION",
       |      "requestedByEmailAddress": "$developer6",
       |      "verificationCode": "pRoPW05BMTQ_HqzTTR0Ent10py9gvstX34_a3dxx4V8",
       |      "updatedOn": 1459868573962
       |    },
       |    "subscriptions": []
       |  }]
    """.stripMargin

  def approvedApplication(description: String = "", verified: Boolean = false) = {
    val verifiedHistory = if (verified) {
      s""",
          |    {
          |      "applicationId": "$approvedApp1",
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
       |    "name": "Application",
       |    "description": "$description",
       |    "collaborators": [
       |      {
       |        "emailAddress": "$adminEmail",
       |        "role": "ADMINISTRATOR"
       |      },
       |      {
       |        "emailAddress": "collaborator@test.com",
       |        "role": "DEVELOPER"
       |      },
       |      {
       |        "emailAddress": "$admin2Email",
       |        "role": "ADMINISTRATOR"
       |      }
       |    ],
       |    "createdOn": 1459866628433,
       |    "redirectUris": [],
       |    "subscriptions": [],
       |    $state
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

  val developerListWithApp =
    s"""
       |[
       |  {
       |    "email": "$developer",
       |    "firstName": "$devFirstName",
       |    "lastName": "$devLastName",
       |    "verified": true
       |  },
       |  {
       |    "email": "$developer2",
       |    "firstName": "$dev2FirstName",
       |    "lastName": "$dev2LastName",
       |    "verified": true
       |  },
       |    {
       |    "email": "$developer3",
       |    "firstName": "$dev3FirstName",
       |    "lastName": "$dev3LastName",
       |    "verified": false
       |
       |  },
       |    {
       |    "email": "$developer4",
       |    "firstName": "$dev4FirstName",
       |    "lastName": "$dev4LastName",
       |    "verified": false
       |  },
       |  {
       |    "email": "$developer5",
       |    "firstName": "$dev5FirstName",
       |    "lastName": "$dev5LastName",
       |    "verified": true
       |
       |  },
       |  {
       |    "email": "$developer6",
       |    "firstName": "$dev6FirstName",
       |    "lastName": "$dev6LastName",
       |    "verified": true
       |  }
       |]
   """.stripMargin

  val developerListWithoutApp =
    s"""
       |[
       |  {
       |    "email": "$developer7",
       |    "firstName": "$dev7FirstName",
       |    "lastName": "$dev7LastName",
       |    "verified": true
       |  },
       |  {
       |    "email": "$developer8",
       |    "firstName": "$dev8FirstName",
       |    "lastName": "$dev8LastName",
       |    "verified": false
       |  },
       |    {
       |    "email": "$developer9",
       |    "firstName": "$dev9FirstName",
       |    "lastName": "$dev9LastName",
       |    "verified": false
       |
       |  },
       |    {
       |    "email": "$developer10",
       |    "firstName": "$dev10FirstName",
       |    "lastName": "$dev10LastName",
       |    "verified": true
       |  },
       |  {
       |    "email": "$developer11",
       |    "firstName": "$dev11FirstName",
       |    "lastName": "$dev11LastName",
       |    "verified": true
       |  }
       |]
   """.stripMargin

  val apiDefinition =
    s"""
    |[
    | {
    |   "serviceName": "employersPayeAPI",
    |   "serviceBaseUrl": "http://employerspaye-api.service/",
    |   "name": "Employers PAYE",
    |   "description": "EMPLOYERS PAYE API.",
    |   "context": "employers-paye",
    |   "versions": [
    |     {
    |       "version": "1.0",
    |       "status": "PUBLISHED",
    |       "access": {
    |         "type": "PUBLIC"
    |       },
    |       "endpoints": [
    |         {
    |           "uriPattern": "/qwerty",
    |           "endpointName": "employersPayeAPI",
    |           "method": "GET",
    |           "authType": "USER",
    |           "throttlingTier": "UNLIMITED",
    |           "scope": "read:employers-paye-1"
    |         }
    |       ]
    |     }
    |   ],
    |   "requiresTrust": false
    | },
    |  {
    |   "serviceName": "payeCreditsAPI",
    |   "serviceBaseUrl": "http://payecredits-api.service/",
    |   "name": "Paye Credits",
    |   "description": "PAYE CREDITS API",
    |   "context": "paye-credits",
    |   "versions": [
    |     {
    |       "version": "1.0",
    |       "status": "DEPRECATED",
    |       "access": {
    |         "type": "PUBLIC"
    |       },
    |       "endpoints": [
    |         {
    |           "uriPattern": "/qwerty",
    |           "endpointName": "payeCreditsAPI",
    |           "method": "GET",
    |           "authType": "USER",
    |           "throttlingTier": "UNLIMITED",
    |           "scope": "read:paye-credits-1"
    |         }
    |       ]
    |     }
    |   ],
    |   "requiresTrust": false
    | },
    |  {
    |   "serviceName": "individualBenefitsAPI",
    |   "serviceBaseUrl": "http://individualbenefits-api.service/",
    |   "name": "Individual Benefits",
    |   "description": "INDIVIDUAL BENEFITS API.",
    |   "context": "individual-benefits",
    |   "versions": [
    |     {
    |       "version": "1.0",
    |       "status": "PUBLISHED",
    |       "access": {
    |         "type": "PUBLIC"
    |       },
    |       "endpoints": [
    |         {
    |           "uriPattern": "/qwerty",
    |           "endpointName": "individualBenefitsAPI",
    |           "method": "GET",
    |           "authType": "USER",
    |           "throttlingTier": "UNLIMITED",
    |           "scope": "read:individual-benefits-1"
    |         }
    |       ]
    |     }
    |   ],
    |   "requiresTrust": false
    | },
    |   {
    |   "serviceName": "selfAssessmentAPI",
    |   "serviceBaseUrl": "http://selfassessment-api.service/",
    |   "name": "Self Assessment",
    |   "description": "SELF ASSESSMENT API.",
    |   "context": "self-assessment",
    |   "versions": [
    |     {
    |       "version": "1.0",
    |       "status": "PUBLISHED",
    |       "access": {
    |         "type": "PUBLIC"
    |       },
    |       "endpoints": [
    |         {
    |           "uriPattern": "/qwerty",
    |           "endpointName": "selfAssessmentAPI",
    |           "method": "GET",
    |           "authType": "USER",
    |           "throttlingTier": "UNLIMITED",
    |           "scope": "read:self-assessment-1"
    |         }
    |       ]
    |     }
    |   ],
    |   "requiresTrust": false
    | }
    |]
  """.stripMargin

  val StringGenerator = (n: Int) => Gen.listOfN(n, Gen.alphaChar).map(_.mkString)

  private val DeveloperGenerator: Gen[User] = for {
    forename <- StringGenerator(5)
    surname <- StringGenerator(5)
    email =  randomEmail
    verified = Option(true)
    registered = None
  } yield User(email, forename, surname, verified, registered)

  def userListGenerator(number:Int): Gen[List[User]] = Gen.listOfN(number, DeveloperGenerator)

  def developerListJsonGenerator(number:Int): Option[String] = userListGenerator(number)
    .sample.map(_.sortWith((userA, userB) => userA.lastName > userB.lastName))
    .map(userList => Json.toJson(userList))
    .map(Json.stringify)


  def administrator(email: String = adminEmail, firstName: String = firstName, lastName: String = lastName) =
    s"""
       |{
       |"email": "$email",
       |"firstName": "$firstName",
       |"lastName": "$lastName",
       |"registrationTime": 1458300873012,
       |"lastModified": 1458300877382,
       |"verified": true
       |}
     """.stripMargin


}
