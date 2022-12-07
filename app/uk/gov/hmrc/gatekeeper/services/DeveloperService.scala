/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.gatekeeper.services

import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors._

import javax.inject.Inject
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.TopicOptionChoice._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DeveloperService @Inject() (
    appConfig: AppConfig,
    developerConnector: DeveloperConnector,
    sandboxApplicationConnector: SandboxApplicationConnector,
    productionApplicationConnector: ProductionApplicationConnector,
    xmlService: XmlService
  )(implicit ec: ExecutionContext
  ) {

  def searchDevelopers(filter: DevelopersSearchFilter)(implicit hc: HeaderCarrier): Future[List[User]] = {

    val unsortedResults: Future[List[User]] = (filter.maybeEmailFilter, filter.maybeApiFilter) match {
      case (emailFilter, None)                 => developerConnector.searchDevelopers(emailFilter, filter.developerStatusFilter)
      case (maybeEmailFilter, Some(apiFilter)) => {
        for {
          collaboratorEmails             <- getCollaboratorsByApplicationEnvironments(filter.environmentFilter, maybeEmailFilter, apiFilter)
          users                          <- developerConnector.fetchByEmails(collaboratorEmails)
          filteredRegisteredUsers        <- Future.successful(users.filter(user => collaboratorEmails.contains(user.email)))
          filteredByDeveloperStatusUsers <- Future.successful(filteredRegisteredUsers.filter(filter.developerStatusFilter.isMatch))
        } yield filteredByDeveloperStatusUsers
      }
    }

    for {
      results <- unsortedResults
    } yield results.sortBy(_.email)
  }

  private def getCollaboratorsByApplicationEnvironments(
      environmentFilter: ApiSubscriptionInEnvironmentFilter,
      maybeEmailFilter: Option[String],
      apiFilter: ApiContextVersion
    )(implicit hc: HeaderCarrier
    ): Future[Set[String]] = {

    val environmentApplicationConnectors = environmentFilter match {
      case ProductionEnvironment => List(productionApplicationConnector)
      case SandboxEnvironment    => List(sandboxApplicationConnector)
      case AnyEnvironment        => List(productionApplicationConnector, sandboxApplicationConnector)
    }

    val allCollaboratorEmailsFutures: List[Future[List[String]]] = environmentApplicationConnectors
      .map(_.searchCollaborators(apiFilter.context, apiFilter.version, maybeEmailFilter))

    combine(allCollaboratorEmailsFutures).map(_.toSet)
  }

  def filterUsersBy(filter: ApiFilter[String], apps: List[Application])(users: List[Developer]): List[Developer] = {

    val registeredEmails = users.map(_.user.email)

    type KEY = Tuple2[String, UserId]

    def asKey(collaborator: Collaborator): KEY = ((collaborator.emailAddress, collaborator.userId))

    def asUnregisteredUser(c: KEY): UnregisteredUser = UnregisteredUser(c._1, c._2)

    def linkAppsAndCollaborators(apps: List[Application]): Map[KEY, Set[Application]] = {
      apps.foldLeft(Map.empty[KEY, Set[Application]])((uMap, appResp) =>
        appResp.collaborators.foldLeft(uMap)((m, c) => {
          val userApps = m.getOrElse(asKey(c), Set.empty[Application]) + appResp
          m + (asKey(c) -> userApps)
        })
      )
    }

    lazy val unregisteredCollaborators: Map[KEY, Set[Application]] =
      linkAppsAndCollaborators(apps).filterKeys(k => !registeredEmails.contains(k._1))

    lazy val unregistered: Set[Developer] =
      unregisteredCollaborators.map {
        case (key, userApps) => Developer(asUnregisteredUser(key), userApps.toList)
      }.toSet

    lazy val (usersWithoutApps, usersWithApps) = users.partition(_.applications.isEmpty)

    filter match {
      case AllUsers                                                                       => users ++ unregistered
      case NoApplications                                                                 => usersWithoutApps
      case NoSubscriptions | OneOrMoreSubscriptions | OneOrMoreApplications | Value(_, _) => usersWithApps ++ unregistered
    }
  }

  def filterUsersBy(filter: StatusFilter)(developers: List[Developer]): List[Developer] = {
    filter match {
      case AnyStatus => developers
      case _         => developers.filter(d => filter == User.status(d.user))
    }
  }

  def getDevelopersWithApps(apps: List[Application], users: List[User]): List[Developer] = {

    def isACollaboratorForApp(user: User)(app: Application): Boolean = app.collaborators.find(_.emailAddress == user.email).isDefined

    def collaboratingApps(user: User): List[Application] = {
      apps.filter(isACollaboratorForApp(user))
    }

    users.map(u => {
      Developer(u, collaboratingApps(u))
    })
  }

  def fetchUsers(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchAll.map(_.sortBy(_.sortField))
  }

  def seekUser(email: String)(implicit hc: HeaderCarrier): Future[Option[User]] = {
    developerConnector.seekUserByEmail(email)
  }

  def fetchOrCreateUser(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    developerConnector.fetchOrCreateUser(email)
  }

  def fetchUser(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    developerConnector.fetchByEmail(email)
  }

  def fetchDeveloper(userId: UserId, includingDeleted: FetchDeletedApplications)(implicit hc: HeaderCarrier): Future[Developer] =
    fetchDeveloper(UuidIdentifier(userId), includingDeleted)

  def fetchDeveloper(developerId: DeveloperIdentifier, includingDeleted: FetchDeletedApplications)(implicit hc: HeaderCarrier): Future[Developer] = {

    def fetchApplicationsByUserId(connector: ApplicationConnector, userId: UserId, includingDeleted: FetchDeletedApplications): Future[List[ApplicationResponse]] = {
      includingDeleted match {
        case FetchDeletedApplications.Include => connector.fetchApplicationsByUserId(userId)
        case FetchDeletedApplications.Exclude => connector.fetchApplicationsExcludingDeletedByUserId(userId)
      }
    }

    for {
      user                   <- developerConnector.fetchById(developerId)
      xmlServiceNames        <- xmlService.getXmlServicesForUser(user.asInstanceOf[RegisteredUser])
      xmlOrganisations       <- xmlService.findOrganisationsByUserId(user.userId)
      sandboxApplications    <- fetchApplicationsByUserId(sandboxApplicationConnector, user.userId, includingDeleted)
      productionApplications <- fetchApplicationsByUserId(productionApplicationConnector, user.userId, includingDeleted)
    } yield Developer(user, (sandboxApplications ++ productionApplications).distinct, xmlServiceNames, xmlOrganisations)
  }

  def fetchDevelopersByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchByEmails(emails)
  }

  def fetchDevelopersByEmailPreferences(topic: TopicOptionChoice, maybeApiCategory: Option[APICategory] = None)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = maybeApiCategory.map(List(_)))
  }

  def fetchDevelopersByAPICategoryEmailPreferences(topic: TopicOptionChoice, apiCategory: APICategory)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = Some(Seq(apiCategory)))
  }

  def fetchDevelopersBySpecificAPIEmailPreferences(
      topic: TopicOptionChoice,
      apiCategories: List[APICategory],
      apiNames: List[String],
      privateApiMatch: Boolean
    )(implicit hc: HeaderCarrier
    ) = {
    developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(apiCategories.distinct), privateApiMatch)
  }

  def removeMfa(developerId: DeveloperIdentifier, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = {
    developerConnector.removeMfa(developerId, loggedInUser)
  }

  def deleteDeveloper(developerId: DeveloperIdentifier, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[(DeveloperDeleteResult, Developer)] = {

    def fetchAdminsToEmail(email: String)(app: Application): Future[Set[String]] = {
      if (app.deployedTo == "SANDBOX") {
        Future.successful(Set.empty)
      } else {
        val appAdmins = app.admins.filterNot(_.emailAddress == email).map(_.emailAddress)
        for {
          users        <- fetchDevelopersByEmails(appAdmins)
          verifiedUsers = users.toSet.filter(_.verified)
          adminsToEmail = verifiedUsers.map(_.email)
        } yield adminsToEmail
      }
    }

    def removeTeamMemberFromApp(email: String)(app: Application) = {
      val connector = if (app.deployedTo == "PRODUCTION") productionApplicationConnector else sandboxApplicationConnector

      for {
        adminsToEmail <- fetchAdminsToEmail(email)(app)
        result        <- connector.removeCollaborator(app.id, email, gatekeeperUserId, adminsToEmail)
      } yield result
    }

    fetchDeveloper(developerId, FetchDeletedApplications.Exclude).flatMap { developer =>
      val email                               = developer.email
      val (appsSoleAdminOn, appsTeamMemberOn) = developer.applications.partition(_.isSoleAdmin(email))

      if (appsSoleAdminOn.isEmpty) {
        for {
          _      <- Future.traverse(appsTeamMemberOn)(removeTeamMemberFromApp(email))
          result <- developerConnector.deleteDeveloper(DeleteDeveloperRequest(gatekeeperUserId, email))
        } yield (result, developer)
      } else {
        Future.successful((DeveloperDeleteFailureResult, developer))
      }
    }
  }

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)
}
