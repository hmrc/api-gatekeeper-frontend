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

package uk.gov.hmrc.gatekeeper.views.developers

import java.time.{Instant, LocalDateTime}
import java.util.UUID

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

import play.twirl.api.HtmlFormat

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationState, Collaborators, State}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.domain.models._
import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.organisations.DeskproOrganisation
import uk.gov.hmrc.gatekeeper.models.xml.{OrganisationId, VendorId, XmlOrganisation}
import uk.gov.hmrc.gatekeeper.testdata.MockDataSugar.deskproOrganisation
import uk.gov.hmrc.gatekeeper.utils.ViewHelpers._
import uk.gov.hmrc.gatekeeper.views.CommonViewSpec
import uk.gov.hmrc.gatekeeper.views.html.developers.DeveloperDetailsView

class DeveloperDetailsViewSpec extends CommonViewSpec with ApplicationBuilder {

  trait Setup {
    val developerDetails = app.injector.instanceOf[DeveloperDetailsView]

    val xmlServiceNames = Set("XML Service 1", "XML Service 2", "XML Service 3")

    val xmlOrganisations = List(XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()), collaborators = List.empty))

    val buildXmlServicesFeUrl: (OrganisationId) => String = (organisationId) =>
      s"/api-gatekeeper-xml-services/organisations/${organisationId.value}"

    val smsMfaDetailVerified: SmsMfaDetail =
      SmsMfaDetail(
        name = "****6789",
        mobileNumber = "0123456789",
        createdOn = Instant.now,
        verified = true
      )

    val smsMfaDetailUnverified = smsMfaDetailVerified.copy(verified = false)

    val authAppMfaDetailVerified: AuthenticatorAppMfaDetail =
      AuthenticatorAppMfaDetail(
        id = MfaId.random,
        name = "Google Auth App",
        createdOn = Instant.now,
        verified = true
      )

    def testDeveloperDetails(developer: Developer) = {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h1", developer.email.text) shouldBe true
      document.getElementById("first-name").text shouldBe developer.firstName
      document.getElementById("last-name").text shouldBe developer.lastName
      document.getElementById("organisations").text shouldBe (developer.deskproOrganisations match {
        case Some(orgs) if orgs.size > 0 => orgs.head.organisationName
        case Some(orgs)                  => ""
        case None                        => "Unavailable"
      })
      document.getElementById("status").text shouldBe (developer.status match {
        case UnverifiedStatus => "not yet verified"
        case VerifiedStatus   => "verified"
        case _                => "unregistered"
      })
      document.getElementById("userId").text shouldBe developer.user.userId.value.toString
      if (developer.xmlServiceNames.isEmpty) {
        document.getElementById("xmlEmailPreferences").text shouldBe "None"
      } else document.getElementById("xmlEmailPreferences").text shouldBe developer.xmlServiceNames.mkString(" ")

      if (developer.xmlOrganisations.isEmpty) {
        document.getElementById("xml-organisation").text shouldBe "None"
      } else {
        val orgId   = developer.xmlOrganisations.map(org => org.organisationId).head
        val orgName = developer.xmlOrganisations.map(org => org.name).head
        document.getElementById("xml-organisation-td").text shouldBe orgName
        document.getElementById("xml-organisation-link").attr("href") shouldBe s"/api-gatekeeper-xml-services/organisations/${orgId.value}"
      }
    }
  }

  "developer details view" should {
    "show unregistered developer details when logged in as superuser" in new Setup {
      val unregisteredDeveloper = Developer(UnregisteredUser("email@example.com".toLaxEmail, UserId.random), List.empty)
      testDeveloperDetails(unregisteredDeveloper)
    }

    "show None Recorded for unregistered developer logged in time when logged in as superuser" in new Setup {
      val unregisteredDeveloper = Developer(UnregisteredUser("email@example.com".toLaxEmail, UserId.random), List.empty)

      val result = developerDetails.render(unregisteredDeveloper, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      document.getElementById("lastLoggedIn").text shouldBe "None Recorded"
    }

    "show unverified developer details when logged in as superuser" in new Setup {
      val unverifiedDeveloper = Developer(RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", false), List.empty)
      testDeveloperDetails(unverifiedDeveloper)
    }

    "show verified developer details when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true), List.empty)
      testDeveloperDetails(verifiedDeveloper)
    }

    "show None Recorded for verified developer who hasn't logged in when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true), List.empty)

      val result = developerDetails.render(verifiedDeveloper, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      document.getElementById("lastLoggedIn").text shouldBe "None Recorded"
    }

    "show Last logged in time for verified developer when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true, lastLogin = Some(instant)), List.empty)

      val result = developerDetails.render(verifiedDeveloper, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")
      document.getElementById("lastLoggedIn").text shouldBe "02 January 2020 03:04"
    }

    "show verified developer details with organisation when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(
        user = RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true),
        applications = List.empty,
        deskproOrganisations = Some(List(DeskproOrganisation(uk.gov.hmrc.gatekeeper.models.organisations.OrganisationId("1"), "Deskpro Organisaion 1", List.empty)))
      )
      testDeveloperDetails(verifiedDeveloper)
    }

    "show verified developer details with no organisations when logged in as superuser" in new Setup {
      val verifiedDeveloper = Developer(
        user = RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true),
        applications = List.empty,
        deskproOrganisations = Some(List.empty)
      )
      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with organisation when logged in as superuser" in new Setup {
      val verifiedDeveloper =
        Developer(
          RegisteredUser("email@example.com".toLaxEmail, UserId.random, "firstname", "lastName", true),
          List.empty,
          xmlServiceNames,
          xmlOrganisations
        )

      testDeveloperDetails(verifiedDeveloper)
    }

    "show developer with no applications when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h2", "Multi-factor authentication") shouldBe true
      elementExistsByText(document, "h2", "Associated applications") shouldBe true
      elementExistsByText(document, "a", "Remove multi-factor authentication") shouldBe false

      document.getElementById("applications").text shouldBe "None"
      document.getElementById("no-mfa").text shouldBe "None"
    }

    "show developer with applications when logged in as superuser" in new Setup {
      val testApplication1 = buildApplication(
        clientId = ClientId("a-client-id"),
        name = Some("appName1"),
        deployedTo = Environment.PRODUCTION,
        collaborators = Set(Collaborators.Administrator(UserId.random, "email@example.com".toLaxEmail)),
        state = ApplicationState(State.TESTING, updatedOn = Instant.now())
      )
      val testApplication2 = buildApplication(
        clientId = ClientId("a-client-id"),
        name = Some("appName2"),
        deployedTo = Environment.PRODUCTION,
        collaborators = Set(Collaborators.Developer(UserId.random, "email@example.com".toLaxEmail)),
        state = ApplicationState(State.PRODUCTION, updatedOn = Instant.now())
      )

      val developerWithApps: Developer = developer.copy(applications = List(testApplication1, testApplication2))

      val result = developerDetails.render(developerWithApps, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      elementExistsByText(document, "h2", "Associated XML vendors") shouldBe true
      elementExistsByText(document, "h2", "Associated applications") shouldBe true
      elementExistsByText(document, "a", "appName1") shouldBe true
      elementExistsByText(document, "a", "appName2") shouldBe true
      document.getElementById("collaborator-0").text shouldBe "Admin"
      document.getElementById("collaborator-1").text shouldBe "Developer"
      document.getElementById("no-mfa").text shouldBe "None"
    }

    "show developer details with delete button when logged in as superuser" in new Setup {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, superUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      document.getElementById("no-mfa").text shouldBe "None"
      elementExistsByText(document, "a", "Remove developer") shouldBe true
    }

    "show developer details without delete button when logged in as non-superuser" in new Setup {
      val result = developerDetails.render(developer, buildXmlServicesFeUrl, strideUserRequest)

      val document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      document.getElementById("no-mfa").text shouldBe "None"
      elementExistsByText(document, "a", "Delete developer") shouldBe false
    }

    "show developer with mfa details and no applications when logged in as superuser" in new Setup {
      val developerWithMfaDetails: Developer = Developer(
        RegisteredUser(
          email = "email@example.com".toLaxEmail,
          userId = UserId.random,
          firstName = "firstname",
          lastName = "lastName",
          verified = true,
          mfaDetails = List(smsMfaDetailUnverified, authAppMfaDetailVerified, smsMfaDetailVerified)
        ),
        applications = List.empty
      )

      val result: HtmlFormat.Appendable = developerDetails.render(developerWithMfaDetails, buildXmlServicesFeUrl, superUserRequest)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      document.getElementById("mfa-heading").text shouldBe "Multi-factor authentication"
      document.getElementById("mfa-type-0").text shouldBe MfaType.AUTHENTICATOR_APP.displayText
      document.getElementById("mfa-type-1").text shouldBe MfaType.SMS.displayText
      document.getElementById("mfa-name-0").text shouldBe s"On (${authAppMfaDetailVerified.name})"
      document.getElementById("mfa-name-1").text shouldBe s"On (${smsMfaDetailVerified.name})"
      document.getElementById("remove-2SV").text shouldBe "Remove multi-factor authentication"
      document.getElementById("remove-2SV").attr("href") shouldBe s"/api-gatekeeper/developer/mfa/remove?developerId=${developerWithMfaDetails.id}"
    }

    "show developer with no mfa details if all unverified and no applications when logged in as superuser" in new Setup {
      val developerWithMfaDetailsUnverified: Developer = Developer(
        RegisteredUser(
          email = "email@example.com".toLaxEmail,
          userId = UserId.random,
          firstName = "firstname",
          lastName = "lastName",
          verified = true,
          mfaDetails = List(smsMfaDetailUnverified)
        ),
        applications = List.empty
      )

      val result: HtmlFormat.Appendable = developerDetails.render(developerWithMfaDetailsUnverified, buildXmlServicesFeUrl, superUserRequest)

      val document: Document = Jsoup.parse(result.body)

      result.contentType should include("text/html")

      document.getElementById("mfa-heading").text shouldBe "Multi-factor authentication"
      document.getElementById("no-mfa").text shouldBe "None"
    }
  }
}
