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

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HeaderCarrier

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.organisations.{OrganisationId, OrganisationWithApps}

@Singleton
class OrganisationService @Inject() (
    apiPlatformDeskproConnector: ApiPlatformDeskproConnector,
    tpoConnector: ThirdPartyOrchestratorConnector
  )(implicit ec: ExecutionContext
  ) extends ApplicationLogger {

  def fetchOrganisationWithApplications(organisationId: OrganisationId)(implicit hc: HeaderCarrier): Future[OrganisationWithApps] = {
    for {
      organisation <- apiPlatformDeskproConnector.getOrganisation(organisationId, hc)
      applications <- tpoConnector.getApplicationsByEmails(organisation.people.map(_.email))
    } yield OrganisationWithApps(organisation.organisationName, applications)
  }
}
