/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.apiplatform.modules.deskpro.services

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.apiplatform.modules.deskpro.connectors.DeskproHorizonConnector
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext

import cats.data.OptionT
import scala.concurrent.Future
import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproOrganisationMembership
import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproPerson
import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproOrganisation
import uk.gov.hmrc.apiplatform.modules.deskpro.models.DeskproOrganisationsResponse

@Singleton
class DeskproHorizonService @Inject()(
  connector: DeskproHorizonConnector
)(
  implicit ec: ExecutionContext
) {
  def addMembership(orgId: Int, email: String)(implicit hc: HeaderCarrier): Future[Option[DeskproOrganisationMembership]] = {
    (for {
      person     <- OptionT(connector.getPerson(email))
      membership <- OptionT(connector.addMembership(orgId, person.id))
    } yield membership).value
  }

  def getMembersOfOrganisation(orgId: Int)(implicit hc: HeaderCarrier): Future[List[DeskproPerson]] = {
    for {
      members <- connector.getMemberships(orgId)
      people  = members.data.map(member => connector.getPerson(member.person))
      p <- Future.sequence(people)
      p2 = p.flatten
    } yield p2
  }

  def getMembershipsOfPerson(email: String)(implicit hc: HeaderCarrier): Future[List[DeskproOrganisation]] = {
    for {
      person              <- connector.getPerson(email)
      organisations       <- connector.getOrganisations().map(response => response.json.as[DeskproOrganisationsResponse].data)
      allMemberships      <- Future.sequence(organisations.map(org => connector.getMemberships(org.id)))
      personMemberships    = person.map(p => allMemberships.flatMap(_.data).filter(_.person == p.id)).getOrElse(List.empty)
      personOrganisations  = personMemberships.flatMap(m => organisations.filter(o => o.id == m.organization))
    } yield (personOrganisations)
  }
}