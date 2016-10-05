/*
 * Copyright 2016 HM Revenue & Customs
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

import connectors.{ApiDefinitionConnector, ApplicationConnector, DeveloperConnector}
import model._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object DeveloperService extends DeveloperService {
  override val developerConnector: DeveloperConnector = DeveloperConnector
  override val apiDefinitionConnector = ApiDefinitionConnector
  override val applicationConnector = ApplicationConnector

}

trait DeveloperService  {

  val developerConnector: DeveloperConnector
  val apiDefinitionConnector: ApiDefinitionConnector
  val applicationConnector: ApplicationConnector

  def filteredApps(filter: ApiFilter[String])(implicit hc: HeaderCarrier): Future[Seq[ApplicationResponse]] = {
    filter match {
      case Value(flt) => applicationConnector.fetchAllApplicationsBySubscription(flt)
      case _ => applicationConnector.fetchAllApplications()
    }
  }

  def getApplicationUsers(filter: ApiFilter[String], allUsers: Seq[User], apps: Seq[ApplicationResponse]): Seq[User] = {
    val collaborators = apps.flatMap(_.collaborators).map(_.emailAddress).toSet
    val users = allUsers.map(u => u.email -> u)(collection.breakOut)
    //val unregistered = collaborators.diff(allUsers.map(_.email).toSet).map(UnregisteredCollaborator(_))
    val registered = filter match {
      case NoSubscriptions => allUsers.filterNot(u => collaborators.contains(u.email))
      case _ => allUsers.filter(u => collaborators.contains(u.email))
    }

    registered // ++ unregistered
  }

  def emailList(users: Seq[User]) = {
    val DELIMITER = "; "  // Outlook requires email addresses separated by semi-colons
    users.map(_.email).mkString(DELIMITER)
  }

  def fetchDevelopers(implicit hc: HeaderCarrier) = {
    developerConnector.fetchAll.map(_.sorted)
  }
}
