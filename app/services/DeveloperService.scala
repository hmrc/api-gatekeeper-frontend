/*
 * Copyright 2018 HM Revenue & Customs
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

import connectors.DeveloperConnector
import model._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.http.HeaderCarrier

object DeveloperService extends DeveloperService {
  override val developerConnector: DeveloperConnector = DeveloperConnector
}

trait DeveloperService {

  val developerConnector: DeveloperConnector

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
      unregisteredCollaborators.map { case(user, apps) =>
        Developer.createUnregisteredDeveloper(user, apps)
      } toSet

    lazy val (usersWithoutApps, usersWithApps) = users.partition(_.apps.isEmpty)

    filter match {
      case AllUsers => users ++ unregistered
      case NoApplications => usersWithoutApps
      case NoSubscriptions | OneOrMoreSubscriptions | OneOrMoreApplications | Value(_) => usersWithApps ++ unregistered
    }
  }

  def filterUsersBy(filter: StatusFilter)(users: Seq[ApplicationDeveloper]): Seq[ApplicationDeveloper] = {
    filter match {
      case AnyStatus => users
      case _ => users.filter(u => u.status == filter)
    }
  }

  def fetchDevelopers(apps: Seq[Application])(implicit hc: HeaderCarrier): Future[Seq[ApplicationDeveloper]] = {

    def collaboratingApps(user: User, apps: Seq[Application]): Seq[Application] = {
      apps.filter(a => a.collaborators.map(col => col.emailAddress).contains(user.email))
    }

    fetchUsers.map(future =>
      future.map(u => {
        Developer.createFromUser(u, collaboratingApps(u, apps))
      }))
  }

  private def fetchUsers(implicit hc: HeaderCarrier): Future[Seq[User]] = {
    developerConnector.fetchAll.map(_.sorted)
  }

  def fetchDeveloper(email: String)(implicit hc: HeaderCarrier): Future[Developer] = {
    developerConnector.fetchByEmail(email).map(_.toDeveloper(Seq()))
  }
}
