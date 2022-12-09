package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.ApplicationResponseBuilder
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models.AbstractApplicationEvent

trait ApplicationEventsTestData extends ApplicationResponseBuilder with CollaboratorsTestData with AccessTestData with ApplicationEventTestDataBuilder with ApplicationStateTestData {

  def sampleEvents(applicationId: ApplicationId) = {
    List[AbstractApplicationEvent](
      makeTeamMemberAddedEvent(applicationId,1),
      makeTeamMemberAddedEvent(applicationId,2)
    )
  }
}
