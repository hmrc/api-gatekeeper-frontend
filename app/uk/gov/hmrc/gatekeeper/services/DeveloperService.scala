/*
 * Copyright 2023 HM Revenue & Customs
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

import java.time.Clock
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponseHelper._
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{ApplicationWithCollaborators, Collaborator}
import uk.gov.hmrc.apiplatform.modules.commands.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actors, LaxEmailAddress, UserId}
import uk.gov.hmrc.apiplatform.modules.common.services.ClockNow
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models._

@Singleton
class DeveloperService @Inject() (
    appConfig: AppConfig,
    developerConnector: DeveloperConnector,
    sandboxApplicationConnector: SandboxApplicationConnector,
    productionApplicationConnector: ProductionApplicationConnector,
    commandConnector: ApplicationCommandConnector,
    deskproConnector: ApiPlatformDeskproConnector,
    xmlService: XmlService,
    val clock: Clock
  )(implicit ec: ExecutionContext
  ) extends ClockNow {

  private def matchTextFilter(user: RegisteredUser, maybeTextFilter: Option[String]): Boolean = {
    maybeTextFilter.fold(true)(text =>
      user.userId.value.toString.toLowerCase().contains(text) ||
        user.email.text.toLowerCase().contains(text) ||
        s"${user.firstName} ${user.lastName}".toLowerCase.contains(text.toLowerCase)
    )
  }

  def searchDevelopers(filter: DevelopersSearchFilter)(implicit hc: HeaderCarrier): Future[List[AbstractUser]] = {

    val unsortedResults: Future[List[AbstractUser]] = (filter.maybeTextFilter, filter.maybeApiFilter) match {
      case (emailFilter, None)                => developerConnector.searchDevelopers(emailFilter, filter.developerStatusFilter)
      case (maybeTextFilter, Some(apiFilter)) => {
        for {
          collaboratorEmails            <- getCollaboratorsByApplicationEnvironments(filter.environmentFilter, apiFilter)
          users                         <- developerConnector.fetchByEmails(collaboratorEmails)
          filteredRegisteredUsers        = users.filter(user => matchTextFilter(user, maybeTextFilter))
          filteredByDeveloperStatusUsers = filteredRegisteredUsers.filter(filter.developerStatusFilter.isMatch)
        } yield filteredByDeveloperStatusUsers
      }
    }

    for {
      results <- unsortedResults
    } yield results.sortBy(_.email.text)
  }

  private def getCollaboratorsByApplicationEnvironments(
      environmentFilter: ApiSubscriptionInEnvironmentFilter,
      apiFilter: ApiContextVersion
    )(implicit hc: HeaderCarrier
    ): Future[Set[LaxEmailAddress]] = {

    val environmentApplicationConnectors = environmentFilter match {
      case ProductionEnvironment => List(productionApplicationConnector)
      case SandboxEnvironment    => List(sandboxApplicationConnector)
      case AnyEnvironment        => List(productionApplicationConnector, sandboxApplicationConnector)
    }

    val allCollaboratorEmailsFutures: List[Future[List[LaxEmailAddress]]] = environmentApplicationConnectors
      .map(_.searchCollaborators(apiFilter.context, apiFilter.versionNbr))

    combine(allCollaboratorEmailsFutures).map(_.toSet)
  }

  def filterUsersBy(filter: ApiFilter[String], apps: List[ApplicationWithCollaborators])(users: List[Developer]): List[Developer] = {

    val registeredEmails = users.map(_.user.email)

    type KEY = Tuple2[LaxEmailAddress, UserId]

    def asKey(collaborator: Collaborator): KEY = ((collaborator.emailAddress, collaborator.userId))

    def asUnregisteredUser(c: KEY): UnregisteredUser = UnregisteredUser(c._1, c._2)

    def linkAppsAndCollaborators(apps: List[ApplicationWithCollaborators]): Map[KEY, Set[ApplicationWithCollaborators]] = {
      apps.foldLeft(Map.empty[KEY, Set[ApplicationWithCollaborators]])((uMap, appResp) =>
        appResp.collaborators.foldLeft(uMap)((m, c) => {
          val userApps = m.getOrElse(asKey(c), Set.empty[ApplicationWithCollaborators]) + appResp
          m + (asKey(c) -> userApps)
        })
      )
    }

    lazy val unregisteredCollaborators: Map[KEY, Set[ApplicationWithCollaborators]] =
      linkAppsAndCollaborators(apps).view.filterKeys(k => !registeredEmails.contains(k._1)).toMap

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
      case _         => developers.filter(d => filter == AbstractUser.status(d.user))
    }
  }

  def getDevelopersWithApps(apps: List[ApplicationWithCollaborators], users: List[AbstractUser]): List[Developer] = {

    def isACollaboratorForApp(user: AbstractUser)(app: ApplicationWithCollaborators): Boolean = app.collaborators.find(_.emailAddress == user.email).isDefined

    def collaboratingApps(user: AbstractUser): List[ApplicationWithCollaborators] = {
      apps.filter(isACollaboratorForApp(user))
    }

    users.map(u => {
      Developer(u, collaboratingApps(u))
    })
  }

  def fetchUsers()(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchAll().map(_.sortBy(_.sortField))
  }

  def fetchUsersPaginated(offset: Int, limit: Int)(implicit hc: HeaderCarrier): Future[UserPaginatedResponse] = {
    developerConnector.fetchAllPaginated(offset, limit)
  }

  def seekUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[Option[AbstractUser]] = {
    developerConnector.seekUserByEmail(email)
  }

  def fetchOrCreateUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[AbstractUser] = {
    developerConnector.fetchOrCreateUser(email)
  }

  def fetchUser(email: LaxEmailAddress)(implicit hc: HeaderCarrier): Future[AbstractUser] = {
    developerConnector.fetchByEmail(email)
  }

  def fetchDeveloper(userId: UserId, includingDeleted: FetchDeletedApplications)(implicit hc: HeaderCarrier): Future[Developer] = {

    def fetchApplicationsByUserId(connector: ApplicationConnector, userId: UserId, includingDeleted: FetchDeletedApplications): Future[List[ApplicationWithCollaborators]] = {
      includingDeleted match {
        case FetchDeletedApplications.Include => connector.fetchApplicationsByUserId(userId)
        case FetchDeletedApplications.Exclude => connector.fetchApplicationsExcludingDeletedByUserId(userId)
      }
    }

    for {
      user                   <- developerConnector.fetchByUserId(userId)
      xmlServiceNames        <- xmlService.getXmlServicesForUser(user.asInstanceOf[RegisteredUser])
      xmlOrganisations       <- xmlService.findOrganisationsByUserId(userId)
      sandboxApplications    <- fetchApplicationsByUserId(sandboxApplicationConnector, userId, includingDeleted)
      productionApplications <- fetchApplicationsByUserId(productionApplicationConnector, userId, includingDeleted)
      deskproOrganisations   <- deskproConnector.getOrganisationsForUser(user.email, hc)
    } yield Developer(user, (sandboxApplications ++ productionApplications).distinct, xmlServiceNames, xmlOrganisations, deskproOrganisations)
  }

  def fetchDevelopersByEmails(emails: Iterable[LaxEmailAddress])(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchByEmails(emails)
  }

  def fetchDevelopersByEmailPreferences(topic: TopicOptionChoice, maybeApiCategory: Option[ApiCategory] = None)(implicit hc: HeaderCarrier): Future[List[RegisteredUser]] = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategories = maybeApiCategory.map(Set(_)))
  }

  def fetchDevelopersByEmailPreferencesPaginated(
      topic: Option[TopicOptionChoice],
      maybeApis: Option[Seq[String]] = None,
      maybeApiCategory: Option[Set[ApiCategory]] = None,
      privateApiMatch: Boolean = false,
      offset: Int,
      limit: Int
    )(implicit hc: HeaderCarrier
    ): Future[UserPaginatedResponse] = {
    developerConnector.fetchByEmailPreferencesPaginated(topic, maybeApis, maybeApiCategories = maybeApiCategory, privateApiMatch, offset, limit)
  }

  def fetchDevelopersByAPICategoryEmailPreferences(topic: TopicOptionChoice, apiCategory: ApiCategory)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferences(topic, maybeApiCategories = Some(Set(apiCategory)))
  }

  def fetchDevelopersBySpecificAPIEmailPreferences(
      topic: TopicOptionChoice,
      apiCategories: Set[ApiCategory],
      apiNames: List[String],
      privateApiMatch: Boolean
    )(implicit hc: HeaderCarrier
    ) = {
    developerConnector.fetchByEmailPreferences(topic, Some(apiNames), Some(apiCategories), privateApiMatch)
  }

  def fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(apiCategories: Set[ApiCategory], offset: Int, limit: Int)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferencesPaginated(None, None, Some(apiCategories), privateapimatch = false, offset, limit)
  }

  def fetchDevelopersBySpecificApisEmailPreferences(apis: List[String], offset: Int, limit: Int)(implicit hc: HeaderCarrier) = {
    developerConnector.fetchByEmailPreferencesPaginated(None, Some(apis.distinct), None, privateapimatch = false, offset, limit)
  }

  def removeMfa(userId: UserId, loggedInUser: String)(implicit hc: HeaderCarrier): Future[RegisteredUser] = {
    developerConnector.removeMfa(userId, loggedInUser)
  }

  def removeEmailPreferencesByService(serviceName: String)(implicit hc: HeaderCarrier): Future[EmailPreferencesDeleteResult] = {
    developerConnector.removeEmailPreferencesByService(serviceName)
  }

  def deleteDeveloper(userId: UserId, gatekeeperUserName: String)(implicit hc: HeaderCarrier): Future[(DeveloperDeleteResult, Developer)] = {

    def fetchAdminsToEmail(filterOutThisEmail: LaxEmailAddress)(app: ApplicationWithCollaborators): Future[Set[LaxEmailAddress]] = {
      if (app.isSandbox) {
        Future.successful(Set.empty)
      } else {
        val appAdmins = app.admins.filterNot(_.emailAddress == filterOutThisEmail).map(_.emailAddress)
        for {
          users        <- fetchDevelopersByEmails(appAdmins)
          verifiedUsers = users.toSet.filter(_.verified)
          adminsToEmail = verifiedUsers.map(_.email)
        } yield adminsToEmail
      }
    }

    def removeTeamMemberFromApp(developer: Developer)(app: ApplicationWithCollaborators): Future[Unit] = {
      val collaborator = app.collaborators.find(_.emailAddress == developer.email).get // Safe as we know we're a dev on this app

      for {
        adminsToEmail <- fetchAdminsToEmail(developer.email)(app)
        cmd            = ApplicationCommands.RemoveCollaborator(Actors.GatekeeperUser(gatekeeperUserName), collaborator, instant())
        result        <- commandConnector.dispatch(app.id, cmd, adminsToEmail).map(_ match {
                           case Left(_)  => throw new RuntimeException("Failed to remove team member from app")
                           case Right(_) => ()
                         })
      } yield result
    }

    fetchDeveloper(userId, FetchDeletedApplications.Exclude).flatMap { developer =>
      val email                               = developer.email
      val (appsSoleAdminOn, appsTeamMemberOn) = developer.applications.partition(_.isSoleAdmin(email))

      if (appsSoleAdminOn.isEmpty) {
        for {
          _      <- Future.traverse(appsTeamMemberOn)(removeTeamMemberFromApp(developer))
          result <- developerConnector.deleteDeveloper(DeleteDeveloperRequest(gatekeeperUserName, email.text))
          _      <- xmlService.removeCollaboratorsForUserId(userId, gatekeeperUserName)
          _      <- deskproConnector.markPersonInactive(email, hc)
        } yield (result, developer)
      } else {
        Future.successful((DeveloperDeleteFailureResult, developer))
      }
    }
  }

  private def combine[T](futures: List[Future[List[T]]]): Future[List[T]] = Future.reduceLeft(futures)(_ ++ _)
}
