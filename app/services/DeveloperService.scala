/*
 * Copyright 2021 HM Revenue & Customs
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

package services

import config.AppConfig
import connectors._
import javax.inject.Inject
import model._
import model.TopicOptionChoice._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DeveloperService @Inject()(appConfig: AppConfig,
                                 developerConnector: DeveloperConnector,
                                 sandboxApplicationConnector: SandboxApplicationConnector,
                                 productionApplicationConnector: ProductionApplicationConnector)(implicit ec: ExecutionContext) {
  def searchDevelopers(filter: Developers2Filter)(implicit hc: HeaderCarrier): Future[Seq[User]] = {


    val unsortedResults: Future[Seq[User]] = (filter.maybeEmailFilter, filter.maybeApiFilter) match {
      case (emailFilter, None) => developerConnector.searchDevelopers(emailFilter, filter.developerStatusFilter)
      case (maybeEmailFilter, Some(apiFilter)) => {
        for {
          collaboratorEmails <- getCollaboratorsByApplicationEnvironments(filter.environmentFilter, maybeEmailFilter, apiFilter)
          users <- developerConnector.fetchByEmails(collaboratorEmails)
          filteredRegisteredUsers <- Future.successful(users.filter(user => collaboratorEmails.contains(user.email)))
          filteredByDeveloperStatusUsers <- Future.successful(filteredRegisteredUsers.filter(filter.developerStatusFilter.isMatch))
        } yield filteredByDeveloperStatusUsers
      }
    }

    for {
      results: Seq[User] <- unsortedResults
    } yield results.sortBy(_.email)
  }

  private def getCollaboratorsByApplicationEnvironments(environmentFilter : ApiSubscriptionInEnvironmentFilter,
                                                        maybeEmailFilter: Option[String],
                                                        apiFilter: ApiContextVersion)
                                                       (implicit hc: HeaderCarrier): Future[Set[String]] = {

    val environmentApplicationConnectors = environmentFilter match {
        case ProductionEnvironment => List(productionApplicationConnector)
        case SandboxEnvironment => List(sandboxApplicationConnector)
        case AnyEnvironment => List(productionApplicationConnector, sandboxApplicationConnector)
      }

    val allCollaboratorEmailsFutures: List[Future[List[String]]] = environmentApplicationConnectors
      .map(_.searchCollaborators(apiFilter.context, apiFilter.version, maybeEmailFilter))

    combine(allCollaboratorEmailsFutures).map(_.toSet)
  }

  def filterUsersBy(filter: ApiFilter[String], apps: Seq[Application])
                   (users: Seq[Developer]): Seq[Developer] = {

    val registeredEmails = users.map(_.email)

    def linkAppsAndCollaborators(apps: Seq[Application]): Map[String, Set[Application]] = {
      apps.foldLeft(Map.empty[String, Set[Application]])((uMap, appResp) =>
        appResp.collaborators.foldLeft(uMap)((m, c) => {
          val userApps = m.getOrElse(c.emailAddress, Set.empty[Application]) + appResp
          m + (c.emailAddress -> userApps)
        }))
    }

    lazy val unregisteredCollaborators: Map[String, Set[Application]] =
      linkAppsAndCollaborators(apps).filterKeys(e => !registeredEmails.contains(e))

    lazy val unregistered: Set[Developer] =
      unregisteredCollaborators.map { case (user, userApps) =>
        Developer.createUnregisteredDeveloper(user, userApps)
      } toSet

    lazy val (usersWithoutApps, usersWithApps) = users.partition(_.apps.isEmpty)

    filter match {
      case AllUsers => users ++ unregistered
      case NoApplications => usersWithoutApps
      case NoSubscriptions | OneOrMoreSubscriptions | OneOrMoreApplications | Value(_, _) => usersWithApps ++ unregistered
    }
  }

  def filterUsersBy(filter: StatusFilter)(users: Seq[Developer]): Seq[Developer] = {
    filter match {
      case AnyStatus => users
      case _ => users.filter(u => u.status == filter)
    }
  }

  def filterUsersBy(filter: ApiSubscriptionInEnvironmentFilter, apps: Seq[Application])(users: Seq[Developer]): Seq[Developer] = filter match {
    case AnyEnvironment => users
    case ProductionEnvironment => users.filter(user => user.apps.exists(app => app.deployedTo == "PRODUCTION"))
    case SandboxEnvironment => users
  }

  def getDevelopersWithApps(apps: Seq[Application], users: Seq[User]): Seq[Developer] = {

    def isACollaboratorForApp(user: User)(app: Application): Boolean = app.collaborators.find(c => c.emailAddress == user.email).isDefined

    def collaboratingApps(user: User): Seq[Application] = {
      apps.filter(isACollaboratorForApp(user))
    }

    users.map(u => {
      Developer.createFromUser(u, collaboratingApps(u))
    })
  }

  def fetchUsers(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchAll.map(_.sorted)
  }

  def fetchUser(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    developerConnector.fetchByEmail(email)
  }

  def fetchDeveloper(email: String)(implicit hc: HeaderCarrier): Future[Developer] = {
    for {
      developer <- developerConnector.fetchByEmail(email)
      sandboxApplications <- sandboxApplicationConnector.fetchApplicationsByEmail(email)
      productionApplications <- productionApplicationConnector.fetchApplicationsByEmail(email)
    } yield Developer.createFromUser(developer, (sandboxApplications ++ productionApplications).distinct)
  }

  def fetchDevelopersByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchByEmails(emails)
  }

  def fetchDevelopersByEmailPreferences(topic: TopicOptionChoice, maybeApiCategory: Option[APICategory] = None)(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = maybeApiCategory.map(Seq(_)))
  }

  def fetchDevelopersByAPICategoryEmailPreferences(topic: TopicOptionChoice, apiCategory: APICategory)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = Some(Seq(apiCategory)))
  }

  def fetchDevelopersBySpecificAPIEmailPreferences(topic: TopicOptionChoice, apiCategories: Seq[APICategory], apiNames: Seq[String])(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(apiCategories.distinct))
  }


  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[User] = {
    developerConnector.removeMfa(email, loggedInUser)
  }

  def deleteDeveloper(email: String, gatekeeperUserId: String)(implicit hc: HeaderCarrier): Future[DeveloperDeleteResult] = {

    def fetchAdminsToEmail(app: Application): Future[Seq[String]] = {
      if (app.deployedTo == "SANDBOX") {
        Future.successful(Seq.empty)
      } else {
        val appAdmins = app.admins.filterNot(_.emailAddress == email).map(_.emailAddress)
        for {
          users <- fetchDevelopersByEmails(appAdmins)
          verifiedUsers = users.filter(_.verified.contains(true))
          adminsToEmail = verifiedUsers.map(_.email)
        } yield adminsToEmail
      }
    }

    def removeTeamMemberFromApp(app: Application) = {
      val connector = if (app.deployedTo == "PRODUCTION") productionApplicationConnector else sandboxApplicationConnector

      for {
        adminsToEmail <- fetchAdminsToEmail(app)
        result <- connector.removeCollaborator(app.id, email, gatekeeperUserId, adminsToEmail)
      } yield result
    }

    fetchDeveloper(email).flatMap { developer =>
      val (appsSoleAdminOn, appsTeamMemberOn) = developer.apps.partition(_.isSoleAdmin(email))

      if (appsSoleAdminOn.isEmpty) {
        for {
          _ <- Future.traverse(appsTeamMemberOn)(removeTeamMemberFromApp)
          result <- developerConnector.deleteDeveloper(DeleteDeveloperRequest(gatekeeperUserId, email))
        } yield result
      } else {
        Future.successful(DeveloperDeleteFailureResult)
      }
    }
  }

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)
}
