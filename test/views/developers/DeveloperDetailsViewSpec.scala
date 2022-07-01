/*
 * Copyright 2022 HM Revenue & Customs
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

package views.developers

import java.util.UUID
import org.jsoup.Jsoup
import utils.ViewHelpers._
import views.html.developers.DeveloperDetailsView
import views.CommonViewSpec
import model._
import model.xml.{OrganisationId, VendorId, XmlOrganisation}


class DeveloperDetailsViewSpec extends CommonViewSpec {

  sealed case class TestApplication(id: ApplicationId,
                                    name: String,
                                    state: ApplicationState,
                                    collaborators: Set[Collaborator],
                                    clientId: ClientId = ClientId("a-client-id"),
                                    deployedTo: String = "PRODUCTION") extends Application

  trait Setup {
    val developerDetails = app.injector.instanceOf[DeveloperDetailsView]

    val xmlServiceNames = Set("XML Service 1", "XML Service 2", "XML Service 3")

    val xmlOrganisations = List(XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID())))

    val buildXmlServicesFeUrl: (OrganisationId) => String = (organisationId) =>
      s"/api-gatekeeper-xml-services/organisations/${organisationId.value}"

    def testDeveloperDetails(developer: Developer) = {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h1", developer.email) shouldBe true
      document.getElementById("first-name").text shouldBe developer.firstName
      document.getElementById("last-name").text shouldBe developer.lastName
      document.getElementById("organisation").text shouldBe (developer.organisation match {
        case Some(text) => text
        case None => ""
      })
      document.getElementById("status").text shouldBe (developer.status match {
        case UnverifiedStatus => "not yet verified"
        case VerifiedStatus => "verified"
        case _ => "unregistered"
      })
      document.getElementById("userId").text shouldBe developer.user.userId.value.toString
      if(developer.xmlEmailPrefServices.isEmpty) {
        document.getElementById("xmlEmailPreferences").text shouldBe "None"
      } else document.getElementById("xmlEmailPreferences").text shouldBe developer.xmlEmailPrefServices.mkString(" ")

      if(developer.xmlOrganisations.isEmpty) {
        document.getElementById("xml-organisation").text shouldBe "None"
      } else {
        val orgId = developer.xmlOrganisations.map(org => org.organisationId).head
        val orgName = developer.xmlOrganisations.map(org => org.name).head
        document.getElementById("xml-organisation-td").text shouldBe orgName
        document.getElementById("xml-organisation-link").attr("href") shouldBe s"/api-gatekeeper-xml-services/organisations/${orgId.value}"
      }
    }
  }

  "developer details view" should {
    "show unregistered developer details when logged in as superuser" in new Setup {
      val unregisteredDeveloper = Developer(UnregisteredUser("email@example.com", UserId.random), List.empty)
      testDeveloperDetails(unregisteredDeveloper)
    }

    "show unverified developer details when logged in as superuser" in new Setup {
      val unverifiedDeveloper = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", false), List.empty)
      testDeveloperDetails(unverifiedDeveloper)
    }

    "show verified developer details when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", true), List.empty)
      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with organisation when logged in as superuser" in new Setup {
      val verifiedDeveloper =
        Developer(
        RegisteredUser("email@example.com", UserId.random, "firstname", "lastName", true, Some("test organisation")),
        List.empty,
        xmlServiceNames,
        xmlOrganisations)

      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with no applications when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer,  buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h2", "Associated applications") shouldBe true
      document.getElementById("applications").text shouldBe "None"
    }

    "show developer with applications when logged in as superuser" in new Setup {
      val testApplication1: TestApplication = TestApplication(ApplicationId(UUID.randomUUID().toString), "appName1", ApplicationState(State.TESTING), Set(Collaborator("email@example.com", CollaboratorRole.ADMINISTRATOR, UserId.random)))
      val testApplication2: TestApplication = TestApplication(ApplicationId(UUID.randomUUID().toString), "appName2", ApplicationState(State.PRODUCTION), Set(Collaborator("email@example.com", CollaboratorRole.DEVELOPER, UserId.random)))

      val developerWithApps: Developer = developer.copy(applications = List(testApplication1, testApplication2))

      val result = developerDetails.render(developerWithApps,  buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h2", "Associated XML organisations") shouldBe true
      elementExistsByText(document, "h2", "Associated applications") shouldBe true
      elementExistsByText(document, "a", "appName1") shouldBe true
      elementExistsByText(document, "td", "Admin") shouldBe true
      elementExistsByText(document, "a", "appName2") shouldBe true
      elementExistsByText(document, "td", "Developer") shouldBe true
    }

    "show developer details with delete button when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "a", "Delete developer") shouldBe true
    }

    "show developer details WITH delete button when logged in as non-superuser" in new Setup {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, strideUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "a", "Delete developer") shouldBe true
    }
  }
}

