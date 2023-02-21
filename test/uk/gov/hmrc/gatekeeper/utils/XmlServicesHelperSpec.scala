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

package uk.gov.hmrc.gatekeeper.utils

import uk.gov.hmrc.gatekeeper.models.xml.XmlApi
import uk.gov.hmrc.gatekeeper.models.{EmailPreferences, EmailTopic, RegisteredUser, TaxRegimeInterests}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId

class XmlServicesHelperSpec extends AnyWordSpec with Matchers with XmlServicesHelper {

  trait Setup {

    def aUser(name: String, verified: Boolean = true, emailPreferences: EmailPreferences = EmailPreferences.noPreferences) = {
      val email  = s"$name@example.com"
      val userId = UserId.random
      RegisteredUser(email, userId, "Fred", "Example", verified, emailPreferences = emailPreferences)
    }

    val xmlApiOne = XmlApi(
      name = "xml api one",
      serviceName = "xml-api-one",
      context = "context",
      description = "description"
    )

    val xmlApiTwo   = xmlApiOne.copy(name = "xml api two", serviceName = "xml-api-two")
    val xmlApiThree = xmlApiOne.copy(name = "xml api three", serviceName = "xml-api-three")
    val xmlApiFour  = xmlApiOne.copy(name = "xml api four", serviceName = "xml-api-four")
    val xmlApis     = List(xmlApiOne, xmlApiTwo, xmlApiThree, xmlApiFour)

    val restApiOne   = "rest-api-one"
    val restApiTwo   = "rest-api-two"
    val restApiThree = "rest-api-three"
    val restApiFour  = "rest-api-four"

    val restEmailPrefInterests = List(TaxRegimeInterests("TestRegimeOne", Set(restApiOne, restApiTwo)), TaxRegimeInterests("TestRegimeTwo", Set(restApiThree, restApiFour)))

    val emailPreferences = EmailPreferences(
      interests = List(
        TaxRegimeInterests("TestRegimeOne", Set(xmlApiOne.serviceName, restApiOne)),
        TaxRegimeInterests("TestRegimeTwo", Set(xmlApiTwo.serviceName, xmlApiThree.serviceName))
      ),
      topics = Set(EmailTopic.TECHNICAL, EmailTopic.BUSINESS_AND_POLICY)
    )

    val restEmailPreferences = emailPreferences.copy(interests = restEmailPrefInterests)

    val user = aUser("Fred", emailPreferences = emailPreferences)
  }

  "filterXmlEmailPreferences" should {
    "return filtered xml service names" in new Setup {
      filterXmlEmailPreferences(emailPreferences.interests, xmlApis) should contain only (xmlApiOne.name, xmlApiTwo.name, xmlApiThree.name)
    }

    "return no xml service names when no xmlApis passed in" in new Setup {
      filterXmlEmailPreferences(emailPreferences.interests, List.empty) shouldBe List.empty
    }

    "return no xml service names when user does not have any email preferences" in new Setup {
      filterXmlEmailPreferences(EmailPreferences.noPreferences.interests, xmlApis) shouldBe List.empty
    }

    "return no xml service names when user does not have xml email preferences" in new Setup {
      filterXmlEmailPreferences(restEmailPreferences.interests, xmlApis) shouldBe List.empty
    }
  }

}
