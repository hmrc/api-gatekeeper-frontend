package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.models.Collaborator
import uk.gov.hmrc.gatekeeper.utils.CollaboratorTracker

trait CollaboratorsTestData extends CommonTestData with CollaboratorTracker {
  val collaboratorsAdminAndUnverifiedDev: Set[Collaborator] = Set(
     administratorEmail.asAdministratorCollaborator,
     developerEmail.asDeveloperCollaborator,
     unverifiedUser.email.asDeveloperCollaborator
  )
  
  val collaboratorsDevAndUnverifiedAdmin: Set[Collaborator] = Set(
     developerEmail.asDeveloperCollaborator,
     unverifiedUser.email.asAdministratorCollaborator
  )
}