package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.ApplicationBuilder
import uk.gov.hmrc.gatekeeper.models.{ClientId, IpAllowlist, RateLimitTier}
import org.joda.time.DateTime

trait ApplicationTestData extends ApplicationBuilder with CommonTestData with CollaboratorsTestData with ApplicationStateTestData with AccessTestData with CheckInformationTestData {

    val defaultApplication = DefaultApplication
       .withId(applicationId)
       .withName(applicationName)
       .withDescription(applicationDescription)
       .withClientId(ClientId("qDxLu6_zZVGurMX7NA7g2Wd5T5Ia"))
       .withGatewayId("12345")
       .deployedToProduction
       .withCollaborators(collaboratorsAdminAndUnverifiedDev)
       .withState(productionState)
       .withAccess(standardAccess)
       .withCheckInformation(defaultCheckInformation)
       .withIpAllowlist(IpAllowlist())
       .unblocked
       .withRateLimitTier(RateLimitTier.BRONZE)
       .withCreatedOn(DateTime.parse("2016-04-08T10:24:40.651Z"))
       .withLastAccess(DateTime.parse("2019-07-01T00:00:00.000Z"))

   val blockedApplication = defaultApplication.withId(blockedApplicationId).withBlocked(true)

   val pendingApprovalApplication = defaultApplication
     .withId(pendingApprovalApplicationId)
     .withState(pendingApprovalState)
     .withName(pendingApprovalApplicationName)

}
