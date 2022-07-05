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

package uk.gov.hmrc.gatekeeper.services

import mocks.connectors.XmlServicesConnectorMockProvider
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.xml.{XmlOrganisation, OrganisationId, VendorId, XmlApi}
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.gatekeeper.utils.AsyncHmrcSpec

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class XmlServiceSpec extends AsyncHmrcSpec {

  trait Setup extends MockitoSugar with ArgumentMatchersSugar with XmlServicesConnectorMockProvider {

    implicit val hc: HeaderCarrier = new HeaderCarrier

    val objectInTest = new XmlService(mockXmlServicesConnector)

    def aUser(name: String, verified: Boolean = true, emailPreferences: EmailPreferences = EmailPreferences.noPreferences) = {
      val email = s"$name@example.com"
      val userId = UserId.random
      RegisteredUser(email, userId, "Fred", "Example", verified, emailPreferences = emailPreferences)
    }

    val xmlApiOne = XmlApi(
      name = "xml api one",
      serviceName = "xml-api-one",
      context = "context",
      description = "description"
    )

    val xmlApiTwo = xmlApiOne.copy(name = "xml api two", serviceName = "xml-api-two")
    val xmlApiThree = xmlApiOne.copy(name = "xml api three", serviceName = "xml-api-three")
    val xmlApiFour = xmlApiOne.copy(name = "xml api four", serviceName = "xml-api-four")
    val xmlApis = Seq(xmlApiOne, xmlApiTwo, xmlApiThree, xmlApiFour)

    val restApiOne = "rest-api-one"

    val emailPreferences = EmailPreferences(
      interests = List(
        TaxRegimeInterests("TestRegimeOne", Set(xmlApiOne.serviceName, restApiOne)),
        TaxRegimeInterests("TestRegimeTwo", Set(xmlApiTwo.serviceName, xmlApiThree.serviceName))
      ),
      topics = Set(EmailTopic.TECHNICAL, EmailTopic.BUSINESS_AND_POLICY)
    )

    val user = aUser("Fred", emailPreferences = emailPreferences)
  }

  "XmlService" when {

    "getXmlServicesForUser" should {

      "Return users xml email preferences when call to get xml apis is successful" in new Setup {
        XmlServicesConnectorMock.GetAllApis.returnsApis(xmlApis)

        val result = await(objectInTest.getXmlServicesForUser(user))

        result should contain only (xmlApiOne.name, xmlApiTwo.name, xmlApiThree.name)
      }

      "Return UpstreamErrorResponse when call to connector fails" in new Setup {
        XmlServicesConnectorMock.GetAllApis.returnsError

        intercept[UpstreamErrorResponse](await(objectInTest.getXmlServicesForUser(user))) match {
          case (e: UpstreamErrorResponse) => succeed
          case _                          => fail
        }
      }
    }

    "findOrganisationsByUserId" should {
      val orgOne = XmlOrganisation(name = "Organisation one", vendorId = VendorId(1), organisationId = OrganisationId(UUID.randomUUID()))

      "Return List of Organisations when call to get xml apis is successful" in new Setup {
        XmlServicesConnectorMock.GetOrganisations.returnsOrganisations(user.userId, List(orgOne))

        val result = await(objectInTest.findOrganisationsByUserId(user.userId))

        result shouldBe List(orgOne)
      }

      "Return UpstreamErrorResponse when call to connector fails" in new Setup {
        XmlServicesConnectorMock.GetOrganisations.returnsError

        intercept[UpstreamErrorResponse](await(objectInTest.findOrganisationsByUserId(user.userId))) match {
          case (e: UpstreamErrorResponse) => succeed
          case _                          => fail
        }
      }
    }
  }
}