package uk.gov.hmrc.gatekeeper.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId

trait ApmConnectorMock {
  self: WiremockSugarIt =>

  def mockApplicationWithSubscriptionData(applicationId: ApplicationId) {
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
