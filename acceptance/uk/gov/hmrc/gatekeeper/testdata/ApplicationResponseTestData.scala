package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.ApplicationResponseBuilder
import uk.gov.hmrc.gatekeeper.models.{ClientId, RateLimitTier}
import org.joda.time.DateTime
import play.api.libs.json.Json
import uk.gov.hmrc.gatekeeper.models.ApplicationResponse

trait ApplicationResponseTestData extends ApplicationResponseBuilder with CollaboratorsTestData with AccessTestData with ApplicationStateTestData {

  val defaultApplicationResponse = DefaultApplicationResponse
    .withId(applicationId)
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
    .withId(blockedApplicationId)
    .withName(blockedApplicationName)
    .withBlocked(true)

  val pendingApprovalApplicationResponse = defaultApplicationResponse
    .withId(pendingApprovalApplicationId)
    .withName(pendingApprovalApplicationName)
    .withState(pendingApprovalState)

  implicit class ApplicationResponseSeqExtension(applicationResponses: Seq[ApplicationResponse]) {
    def toJson = Json.toJson(applicationResponses)
    def toJsonString = Json.toJson(applicationResponses).toString
  }
}
