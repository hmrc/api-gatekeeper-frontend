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

import uk.gov.hmrc.gatekeeper.connectors.XmlServicesConnector
import uk.gov.hmrc.gatekeeper.models.xml.XmlOrganisation
import uk.gov.hmrc.gatekeeper.models.{RegisteredUser, UserId}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.gatekeeper.utils.XmlServicesHelper

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class XmlService @Inject() (xmlServicesConnector: XmlServicesConnector)(implicit val ec: ExecutionContext) extends XmlServicesHelper {

  def getXmlServicesForUser(user: RegisteredUser)(implicit hc: HeaderCarrier): Future[Set[String]] = {
    xmlServicesConnector.getAllApis().map(apis => filterXmlEmailPreferences(user, apis))
  }

  def findOrganisationsByUserId(userId: UserId)(implicit hc: HeaderCarrier): Future[List[XmlOrganisation]] =  {
    xmlServicesConnector.findOrganisationsByUserId(userId)
  }

}