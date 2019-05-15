/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class DeveloperService @Inject()(appConfig: AppConfig,
                                 developerConnector: DeveloperConnector,
                                 sandboxApplicationConnector: SandboxApplicationConnector,
                                 productionApplicationConnector: ProductionApplicationConnector)(implicit ec: ExecutionContext) {

  def searchDevelopers(filter: Developers2Filter)(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    (filter.maybeEmailFilter, filter.maybeApiFilter) match {
      case (None, None) => searchDevelopersByEmailFilter("")
      case (Some(emailFilter), None) => searchDevelopersByEmailFilter(emailFilter)
      case (Some(_), Some(_)) => throw new NotImplementedError("Currently does not support subscription and email filtering")
      case (None, Some(apiFilter)) => {
        val allEmails = for {
          emailsProduction <- productionApplicationConnector.searchCollaborators2(apiFilter.context, apiFilter.version)
          emailsSandbox <- sandboxApplicationConnector.searchCollaborators2(apiFilter.context, apiFilter.version)
        } yield (emailsProduction ++ emailsSandbox)

        for{
          emails <- allEmails
        } yield emails.distinct.map(email => User(email, firstName = "", lastName = "" , verified = None))
      }
    }
  }

  def filterUsersBy(filter: ApiFilter[String], apps: Seq[Application])
                   (users: Seq[ApplicationDeveloper]): Seq[ApplicationDeveloper] = {

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

  def filterUsersBy(filter: StatusFilter)(users: Seq[ApplicationDeveloper]): Seq[ApplicationDeveloper] = {
    filter match {
      case AnyStatus => users
      case _ => users.filter(u => u.status == filter)
    }
  }

  def filterUsersBy(filter: ApiSubscriptionInEnvironmentFilter, apps: Seq[Application])(users: Seq[ApplicationDeveloper]): Seq[ApplicationDeveloper] = filter match {
    case AnyEnvironment => users
    case ProductionEnvironment => users.filter(user => user.apps.exists(app => app.deployedTo == "PRODUCTION"))
    case SandboxEnvironment => users
  }

  def getDevelopersWithApps(apps: Seq[Application], users: Seq[User])(implicit hc: HeaderCarrier): Seq[ApplicationDeveloper] = {

    def collaboratingApps(user: User, apps: Seq[Application]): Seq[Application] = {
      apps.filter(a => a.collaborators.map(col => col.emailAddress).contains(user.email))
    }

    users.map(u => {
      Developer.createFromUser(u, collaboratingApps(u, apps))
    })
  }

  def fetchUsers(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchAll.map(_.sorted)
  }

  def fetchUser(email: String)(implicit hc: HeaderCarrier): Future[User] = {
    developerConnector.fetchByEmail(email)
  }

  def fetchDeveloper(email: String)(implicit hc: HeaderCarrier): Future[ApplicationDeveloper] = {
    for {
      developer <- developerConnector.fetchByEmail(email)
      sandboxApplications <- sandboxApplicationConnector.fetchApplicationsByEmail(email)
      productionApplications <- productionApplicationConnector.fetchApplicationsByEmail(email)
    } yield Developer.createFromUser(developer, (sandboxApplications ++ productionApplications).distinct)
  }

  def fetchDevelopersByEmails(emails: Iterable[String])(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchByEmails(emails)
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
        result <- connector.removeCollaborator(app.id.toString, email, gatekeeperUserId, adminsToEmail)
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

  private def searchDevelopersByEmailFilter(emailFilter: String)(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    for {
      developers <- developerConnector.searchDevelopers(emailFilter)
      productionCollaboratorsEmails <- productionApplicationConnector.searchCollaborators(emailFilter)
      productionCollaborators <- Future.successful(productionCollaboratorsEmails.map(UnregisteredCollaborator.apply))
      sandboxCollaboratorsEmails <- sandboxApplicationConnector.searchCollaborators(emailFilter)
      sandboxCollaborators <- Future.successful(sandboxCollaboratorsEmails.map(UnregisteredCollaborator.apply))
    } yield developers ++ productionCollaborators ++ sandboxCollaborators
  }
}
