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
  def searchDevelopers(filter: Developers2Filter)(implicit hc: HeaderCarrier): Future[Seq[NewModel.User]] = {

    val unsortedResults: Future[Seq[NewModel.User]] = (filter.maybeEmailFilter, filter.maybeApiFilter) match {
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
      results <- unsortedResults
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
                   (users: Seq[NewModel.Developer]): Seq[NewModel.Developer] = {

    val registeredEmails = users.map(_.user.email)

    def linkAppsAndCollaborators(apps: Seq[Application]): Map[String, Set[Application]] = {
      apps.foldLeft(Map.empty[String, Set[Application]])((uMap, appResp) =>
        appResp.collaborators.foldLeft(uMap)((m, c) => {
          val userApps = m.getOrElse(c.emailAddress, Set.empty[Application]) + appResp
          m + (c.emailAddress -> userApps)
        }))
    }

    lazy val unregisteredCollaborators: Map[String, Set[Application]] =
      linkAppsAndCollaborators(apps).filterKeys(e => !registeredEmails.contains(e))

    lazy val unregistered: Set[NewModel.Developer] =
      unregisteredCollaborators.map { case (email, userApps) => NewModel.Developer(NewModel.UnregisteredUser(email), userApps.toSeq) } toSet

    lazy val (usersWithoutApps, usersWithApps) = users.partition(_.applications.isEmpty)

    filter match {
      case AllUsers => users ++ unregistered
      case NoApplications => usersWithoutApps
      case NoSubscriptions | OneOrMoreSubscriptions | OneOrMoreApplications | Value(_, _) => usersWithApps ++ unregistered
    }
  }

  def filterUsersBy(filter: StatusFilter)(developers: Seq[NewModel.Developer]): Seq[NewModel.Developer] = {
    filter match {
      case AnyStatus => developers
      case _ => developers.filter(d => filter == NewModel.User.status(d.user))
    }
  }

  def getDevelopersWithApps(apps: Seq[Application], users: Seq[NewModel.User]): Seq[NewModel.Developer] = {

    def isACollaboratorForApp(user: NewModel.User)(app: Application): Boolean = app.collaborators.find(_.emailAddress == user.email).isDefined

    def collaboratingApps(user: NewModel.User): Seq[Application] = {
      apps.filter(isACollaboratorForApp(user))
    }

    users.map(u => {
      NewModel.Developer(u, collaboratingApps(u))
    })
  }

  def fetchUsers(implicit hc: HeaderCarrier): Future[Seq[NewModel.RegisteredUser]] = {
    developerConnector.fetchAll.map(_.sortBy(_.sortField))
  }

  def fetchUser(email: String)(implicit hc: HeaderCarrier): Future[NewModel.User] = {
    developerConnector.fetchByEmail(email)
  }

  def fetchDeveloper(email: String)(implicit hc: HeaderCarrier): Future[NewModel.Developer] = {
    for {
      user <- developerConnector.fetchByEmail(email)
      sandboxApplications <- sandboxApplicationConnector.fetchApplicationsByEmail(email)
      productionApplications <- productionApplicationConnector.fetchApplicationsByEmail(email)
    } yield NewModel.Developer(user, (sandboxApplications ++ productionApplications).distinct)
  }

  def fetchDevelopersByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[NewModel.RegisteredUser]] = {
    developerConnector.fetchByEmails(emails)
  }

  def fetchDevelopersByEmailPreferences(topic: TopicOptionChoice, maybeApiCategory: Option[APICategory] = None)(implicit hc: HeaderCarrier): Future[Seq[NewModel.RegisteredUser]] = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = maybeApiCategory.map(Seq(_)))
  }

  def fetchDevelopersByAPICategoryEmailPreferences(topic: TopicOptionChoice, apiCategory: APICategory)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategory = Some(Seq(apiCategory)))
  }

  def fetchDevelopersBySpecificAPIEmailPreferences(topic: TopicOptionChoice, apiCategories: Seq[APICategory], apiNames: Seq[String])(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(apiCategories.distinct))
  }


  def removeMfa(email: String, loggedInUser: String)(implicit hc: HeaderCarrier): Future[NewModel.RegisteredUser] = {
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
          verifiedUsers = users.filter(_.verified)
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
      val (appsSoleAdminOn, appsTeamMemberOn) = developer.applications.partition(_.isSoleAdmin(email))

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
