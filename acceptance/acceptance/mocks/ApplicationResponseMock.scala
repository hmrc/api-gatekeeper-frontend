package acceptance.mocks

import builder.ApplicationResponseBuilder
import model.ApplicationId
import model.ClientId
import model.RateLimitTier
import org.joda.time.DateTime
import play.api.libs.json.Json
import model.ApplicationResponse

trait ApplicationResponseMock extends ApplicationResponseBuilder with CollaboratorsMock with AccessMock with ApplicationStateMock {

  val applicationResponseTest = DefaultApplicationResponse
      .withId(ApplicationId(newApplicationWithSubscriptionDataId))
      .withName(newApplicationName)
      .withDescription("application for test")
      .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
      .withGatewayId("12345")
      .deployedToProduction
      .withCollaborators(collaboratorsForFetchAppResponseByEmail)
      .withState(testStateForFetchAppResponseByEmail)
      .withAccess(testAccess)
      .unblocked
      .withRateLimitTier(RateLimitTier.BRONZE)
      .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
      .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

  val blockedApplicationResponse = applicationResponseTest.copy(id = ApplicationId(newBlockedApplicationWithSubscriptionDataId), blocked = true, name = newBlockedApplicationName)

  implicit class ApplicationResponseSeqExtension(applicationResponses: Seq[ApplicationResponse]) {
    def toJson = Json.toJson(applicationResponses)
    def toJsonString = Json.toJson(applicationResponses).toString
  }
}