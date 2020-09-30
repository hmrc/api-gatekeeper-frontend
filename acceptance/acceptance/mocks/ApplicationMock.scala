package acceptance.mocks

import builder.ApplicationBuilder
import model.ApplicationId
import model.ClientId
import model.RateLimitTier
import org.joda.time.DateTime

trait ApplicationMock extends ApplicationBuilder with TestData with CollaboratorsMock with ApplicationStateMock with AccessMock with CheckInformationMock {

    val testIpWhitelist = Set.empty[String]

    val testApplication = DefaultApplication
       .withId(ApplicationId(newApplicationWithSubscriptionDataId))
       .withName(newApplicationName)
       .withDescription(newApplicationDescription)
       .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
       .withGatewayId("12345")
       .deployedToProduction
       .withCollaborators(collaborators)
       .withState(testState)
       .withAccess(testAccess)
       .withCheckInformation(testCheckInformation)
       .allowIPs(testIpWhitelist.toSeq :_*)
       .unblocked
       .withRateLimitTier(RateLimitTier.BRONZE)
       .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
       .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

   val blockedApplicationTest = testApplication.withId(ApplicationId(newBlockedApplicationWithSubscriptionDataId)).withBlocked(true)

   val pendingApprovalApplicationTest = testApplication
     .withId(ApplicationId(newPendingApprovalApplicationWithSubscriptionDataId))
     .withState(pendingApprovalState)
     .withName(newPendingApprovalApplicationName)

}