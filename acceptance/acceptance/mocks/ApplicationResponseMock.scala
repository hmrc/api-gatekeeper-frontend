package acceptance.mocks

import builder.ApplicationResponseBuilder
import model.{ApplicationId, ClientId, RateLimitTier}
import org.joda.time.DateTime
import play.api.libs.json.Json
import model.ApplicationResponse

trait ApplicationResponseMock extends ApplicationResponseBuilder with CollaboratorsMock with AccessMock with ApplicationStateMock {

  val defaultApplicationResponse = DefaultApplicationResponse
    .withId(ApplicationId(applicationId))
    .withName(applicationName)
    .withDescription("application for test")
    .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
    .withGatewayId("12345")
    .deployedToProduction
    .withCollaborators(collaboratorsDevAndUnverifiedAdmin)
    .withState(stateForFetchAppResponseByEmail)
    .withAccess(standardAccess)
    .unblocked
    .withRateLimitTier(RateLimitTier.BRONZE)
    .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
    .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

  val blockedApplicationResponse = defaultApplicationResponse
    .withId(ApplicationId(blockedApplicationId))
    .withName(blockedApplicationName)
    .withBlocked(true)

  val pendingApprovalApplicationResponse = defaultApplicationResponse
    .withId(ApplicationId(pendingApprovalApplicationId))
    .withName(pendingApprovalApplicationName)
    .withState(pendingApprovalState)

  implicit class ApplicationResponseSeqExtension(applicationResponses: Seq[ApplicationResponse]) {
    def toJson = Json.toJson(applicationResponses)
    def toJsonString = Json.toJson(applicationResponses).toString
  }
}
