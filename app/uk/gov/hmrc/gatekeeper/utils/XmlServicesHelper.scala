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

package uk.gov.hmrc.gatekeeper.utils

import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.models.xml.XmlApi

trait XmlServicesHelper {

  def filterXmlEmailPreferences(user: RegisteredUser, xmlApis: Seq[XmlApi]): Set[String] = {
    val services: Set[String] = xmlApis.map(x => x.serviceName).toSet
    val userServices = user.emailPreferences.interests.flatMap(interest => interest.services).toSet
    val filteredServices: Set[String] = services.intersect(userServices)

    xmlApis
      .filter(api => filteredServices.contains(api.serviceName))
      .map(x => x.name).toSet
  }

}
