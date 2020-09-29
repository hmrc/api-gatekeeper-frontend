package acceptance.mocks

import model.Collaborator
import model.CollaboratorRole

trait CollaboratorsMock extends TestData {
  val collaborators: Set[Collaborator] = Set(
     Collaborator(newAdminEmail, CollaboratorRole.ADMINISTRATOR),
     Collaborator(newDeveloper, CollaboratorRole.DEVELOPER),
     Collaborator(unverifiedUser.email, CollaboratorRole.DEVELOPER)
  )
  
  val collaboratorsForFetchAppResponseByEmail: Set[Collaborator] = Set(
     Collaborator(newDeveloper, CollaboratorRole.DEVELOPER),
     Collaborator(unverifiedUser.email, CollaboratorRole.ADMINISTRATOR)
  )
}
