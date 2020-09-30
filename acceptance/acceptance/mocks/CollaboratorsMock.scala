package acceptance.mocks

import model.Collaborator
import model.CollaboratorRole

trait CollaboratorsMock extends TestData {
  val collaboratorsAdminAndUnverifiedDev: Set[Collaborator] = Set(
     Collaborator(administratorEmail, CollaboratorRole.ADMINISTRATOR),
     Collaborator(developerEmail, CollaboratorRole.DEVELOPER),
     Collaborator(unverifiedUser.email, CollaboratorRole.DEVELOPER)
  )
  
  val collaboratorsDevAndUnverifiedAdmin: Set[Collaborator] = Set(
     Collaborator(developerEmail, CollaboratorRole.DEVELOPER),
     Collaborator(unverifiedUser.email, CollaboratorRole.ADMINISTRATOR)
  )
}
