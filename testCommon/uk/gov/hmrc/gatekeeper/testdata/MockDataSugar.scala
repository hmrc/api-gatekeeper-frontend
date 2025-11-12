/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.testdata

import java.util.UUID

import org.scalacheck.Gen

import play.api.libs.json.Json

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithSubscriptionsFixtures, Collaborators}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{ApplicationId, LaxEmailAddress, OrganisationIdFixtures, UserId}
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.models.organisations.DeskproOrganisation
import uk.gov.hmrc.gatekeeper.models.xml.{OrganisationId, VendorId, XmlApi, XmlOrganisation}

object MockDataSugar extends ApplicationWithSubscriptionsFixtures {
  val approvedApp1 = ApplicationId.unsafeApply("df0c32b6-bbb7-46eb-ba50-e6e5459162ff")
  val approvedApp2 = ApplicationId.unsafeApply("a4b47c82-5888-41fd-aa83-da2bbd4679d1")
  val approvedApp3 = ApplicationId.unsafeApply("9688ad02-230e-42b7-8f9a-be593565bfdc")
  val approvedApp4 = ApplicationId.unsafeApply("56148b28-65b0-47dd-a3ce-2f02840ddd31")
  val appToDelete  = ApplicationId.unsafeApply("fa38d130-7c8e-47d8-abc0-0374c7f73216")

  val adminEmail  = "admin@example.com"
  val adminId     = UUID.fromString("5ed7ea4a-e2bd-41ba-8db1-5c81f323ab49")
  val admin2Email = "admin2@example.com"
  val admin2Id    = UUID.fromString("be56d8e8-d040-4927-bd9c-d0d5c9934f6b")
  val firstName   = "John"
  val lastName    = "Test"

  val developer    = "purnima.fakename@example.com"
  val devFirstName = "Purnima"
  val devLastName  = "Fakename"
  val developerId  = UUID.fromString("984dede9-d3b7-471c-b96b-6a38dd17748c")

  val developer2    = "imran.fakename@example.com"
  val developer2Id  = UUID.fromString("8ecb1b81-72df-4d84-9449-051bd7fe21b5")
  val dev2FirstName = "Imran"
  val dev2LastName  = "Fakename"

  val developer4    = "a.long.name.jane.hayjdjdu@a-very-long-email-address-exampleifi.com"
  val developer4Id  = UUID.fromString("cb827985-c65c-49bb-8c0c-7a974aa68c0d")
  val dev4FirstName = "HannahHmrcSdstusercollaboratir"
  val dev4LastName  = "Kassidyhmrcdevusercollaborato"

  val developer5    = "john.fakename@example.com"
  val developer5Id  = UUID.fromString("1fe7b18e-a460-4936-b99f-fb0269b829e6")
  val dev5FirstName = "John"
  val dev5LastName  = "Fakename"

  val developer6    = "vijaya.fakename@example.com"
  val developer6Id  = UUID.fromString("d0f49957-cbeb-443d-a8a0-82c2f343d427")
  val dev6FirstName = "Vijaya"
  val dev6LastName  = "Fakename"

  val developer7    = "kerri.fakename@example.com"
  val developer7Id  = UUID.fromString("8b0c588d-4300-4dcd-a185-0c94431d2cdb")
  val dev7FirstName = "Kerri"
  val dev7LastName  = "Fakename"

  val developer8    = "dixie.fakename@example.com"
  val developer8Id  = UUID.fromString("6579f294-498f-4277-bde0-1dab2b71f212")
  val dev8FirstName = "Dixie"
  val dev8LastName  = "Fakename"

  val developer9   = "fred@example.com"
  val developer9Id = UUID.fromString("ef6974ab-2eb7-4266-82f7-c71a6915db19")
  val dev9name     = "n/a"

  val developer10   = "peter.fakename@example.com"
  val developer10Id = UUID.fromString("4cde4666-fd69-4847-89d3-9eb852ee8cdf")
  val dev10name     = "n/a"

  val randomEmail = s"john.smith${System.currentTimeMillis}@example.com"

  val statusVerified     = "verified"
  val statusUnverified   = "not yet verified"
  val statusUnregistered = "not registered"

  val xmlApiOne = XmlApi(
    name = "xml api one",
    serviceName = "xml-api-one",
    context = "context",
    description = "description"
  )

  val xmlApis          = Json.toJson(Seq(xmlApiOne)).toString
  val orgOne           = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()), collaborators = List.empty)
  val xmlOrganisations = Json.toJson(List(orgOne)).toString

  val deskproOrganisationId = uk.gov.hmrc.gatekeeper.models.organisations.OrganisationId("1")
  val deskproOrganisation   = DeskproOrganisation(deskproOrganisationId, "Deskpro organisation 1", List.empty)

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

  val approvedApp1Model = standardApp
    .withId(approvedApp1)
    .withCollaborators(
      Collaborators.Administrator(UserId(developerId), LaxEmailAddress(developer)),
      Collaborators.Developer(UserId(developer9Id), LaxEmailAddress(developer9))
    )
    .withSubscriptions(Set.empty)

  val approvedApp2Model = standardApp2
    .withId(approvedApp2)
    .withCollaborators(
      Collaborators.Administrator(UserId(developer2Id), LaxEmailAddress(developer2)),
      Collaborators.Developer(UserId(developer7Id), LaxEmailAddress(developer7)),
      Collaborators.Developer(UserId(developer8Id), LaxEmailAddress(developer8))
    )
    .withSubscriptions(Set.empty)

  val applicationResponse = Json.toJson(
    List(
      approvedApp1Model,
      approvedApp2Model
    )
  ).toString

  val applicationResponseForEmail = Json.toJson(List(standardApp
    .withId(appToDelete)
    .withCollaborators(
      Collaborators.Administrator(UserId(developer8Id), LaxEmailAddress(developer8)),
      Collaborators.Developer(UserId(developer9Id), LaxEmailAddress(developer9))
    )
    .withSubscriptions(Set.empty))).toString()

  val applicationResponsewithNoSubscription = Json.toJson(List(standardApp
    .withId(approvedApp1)
    .withCollaborators(
      Collaborators.Administrator(UserId(developer4Id), LaxEmailAddress(developer4)),
      Collaborators.Developer(UserId(developer5Id), LaxEmailAddress(developer5)),
      Collaborators.Developer(UserId(developer6Id), LaxEmailAddress(developer6)),
      Collaborators.Developer(UserId(developer10Id), LaxEmailAddress(developer10))
    )
    .withSubscriptions(Set.empty))).toString()

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
       |    "mfaDetails":[
       |    {"id":"f64b240e-ee7a-40ae-8eeb-b2a151da00cd",
       |    "name":"Google Auth App",
       |    "createdOn":"2022-07-19T07:07:45.661Z",
       |    "verified":true,
       |    "mfaType":"AUTHENTICATOR_APP"},
       |    {"id":"12b240e-ee7a-40ae-8eeb-b2a151da00de",
       |    "name":"****6789",
       |    "createdOn":"2022-08-26T13:42:39.441Z",
       |    "mobileNumber": "0123456789",
       |    "verified":true,
       |    "mfaType":"SMS"}
       |    ],
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
       |    "mfaDetails":[
       |    {"id":"f64b240e-ee7a-40ae-8eeb-b2a151da00cd",
       |    "name":"Google Auth App",
       |    "createdOn":"2022-07-19T07:07:45.661Z",
       |    "verified":true,
       |    "mfaType":"AUTHENTICATOR_APP"},
       |    {"id":"12b240e-ee7a-40ae-8eeb-b2a151da00de",
       |    "name":"****6789",
       |    "createdOn":"2022-08-26T13:42:39.441Z",
       |    "mobileNumber": "0123456789",
       |    "verified":true,
       |    "mfaType":"SMS"}
       |    ],
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  }
   """.stripMargin

  val userWithoutMfaDetails =
    s"""
       |  {
       |    "email": "$developer8",
       |    "userId": "$developer8Id",
       |    "firstName": "$dev8FirstName",
       |    "lastName": "$dev8LastName",
       |    "verified": false,
       |    "mfaEnabled": true,
       |    "mfaDetails":[],
       |    "emailPreferences": {
       |      "interests": [],
       |      "topics": []
       |    }
       |  }
   """.stripMargin

  val StringGenerator = (n: Int) => Gen.listOfN(n, Gen.alphaChar).map(_.mkString)

  private val DeveloperGenerator: Gen[RegisteredUser] = for {
    forename  <- StringGenerator(5)
    surname   <- StringGenerator(5)
    id         = UserId.random.value
    email      = randomEmail
    verified   = true
    registered = None
  } yield RegisteredUser(email.toLaxEmail, UserId(id), forename, surname, verified)

  def userListGenerator(number: Int): Gen[List[RegisteredUser]] = Gen.listOfN(number, DeveloperGenerator)

  def developerListJsonGenerator(number: Int): Option[String] =
    userListGenerator(number)
      .sample
      .map(_.sortWith((userA, userB) => userA.lastName > userB.lastName))
      .map(userList => Json.toJson(userList))
      .map(Json.stringify)

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

  val applicationForDeveloperResponse: String = Json.toJson(
    List(
      standardApp
    )
  )
    .toString()
}
