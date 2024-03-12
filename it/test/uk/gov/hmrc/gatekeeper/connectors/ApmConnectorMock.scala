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

package uk.gov.hmrc.gatekeeper.connectors

import com.github.tomakehurst.wiremock.client.WireMock._

import play.api.http.Status._

import uk.gov.hmrc.apiplatform.modules.common.domain.models.ApplicationId

trait ApmConnectorMock {
  self: WiremockSugarIt =>

  def mockApplicationWithSubscriptionData(applicationId: ApplicationId): Unit = {
    stubFor(get(urlEqualTo(s"/applications/${applicationId.value.toString()}"))
      .willReturn(
        aResponse()
          .withStatus(OK)
          .withBody(
            s"""{
               |   "application": {
               |       "id": "${applicationId.value.toString()}",
               |       "clientId": "qDxLu6_zZVGurMX7NA7g2Wd5T5Ia",
               |       "blocked": false,
               |       "gatewayId": "12345",
               |       "rateLimitTier": "BRONZE",
               |       "name": "My new app",
               |       "createdOn": "2016-04-08T10:24:40.651Z",
               |       "lastAccess": "2019-07-01T00:00:00.000Z",
               |       "deployedTo": "PRODUCTION",
               |       "description": "my description",
               |       "collaborators": [
               |           {
               |               "emailAddress": "thomas.vandevelde@digital.hmrc.gov.uk",
               |               "role": "ADMINISTRATOR"
               |           }
               |       ],
               |       "access": {
               |           "redirectUris": [
               |               "http://localhost:8080/callback"
               |           ],
               |           "termsAndConditionsUrl": "http://terms",
               |           "privacyPolicyUrl": "http://privacypolicy",
               |           "overrides": [],
               |           "accessType": "STANDARD"
               |       },
               |       "state": {
               |       "name": "PRODUCTION",
               |       "requestedByEmailAddress": "thomas.vandevelde@digital.hmrc.gov.uk",
               |       "verificationCode": "8mmsC_z9G-rRjt2cjnYP7q9r7aVbmS5cfGv_M-09kdw",
               |       "updatedOn": "2016-04-08T11:11:18.463Z"
               |       },
               |       "ipAllowlist": {
               |         "required": false,
               |         "allowlist": []
               |       }
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
          )
      ))
  }
}
