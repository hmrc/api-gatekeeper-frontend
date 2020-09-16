/*
 * Copyright 2020 HM Revenue & Customs
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

package builder

import model.applications.NewApplication
import model.ApplicationId
import model.ClientId
import uk.gov.hmrc.time.DateTimeUtils
import model.Environment
import model.Standard
import model.State
import model.Collaborator
import model.applications.ApplicationWithSubscriptionData
import model.CollaboratorRole


trait ApplicationBuilder {

  def buildApplication(appId: ApplicationId): NewApplication = {

    val clientId = ClientId.random
    val appOwnerEmail = "a@b.com"

    NewApplication(
      appId,
      clientId,
      s"$appId-name",
      DateTimeUtils.now,
      DateTimeUtils.now,
      None,
      Environment.SANDBOX,
      Some(s"$appId-description"),
      buildCollaborators(Seq(appOwnerEmail)),
      state = State.PRODUCTION,
      access = Standard(
        redirectUris = Seq("https://red1", "https://red2"),
        termsAndConditionsUrl = Some("http://tnc-url.com")
      )
    )
  }

  def buildCollaborators(emails: Seq[String]): Set[Collaborator] = {
    emails.map(email => Collaborator(email, CollaboratorRole.ADMINISTRATOR)).toSet
  }

  def buildApplicationWithSubscriptionData(): ApplicationWithSubscriptionData = {
    val application = buildApplication(ApplicationId.random)

    ApplicationWithSubscriptionData(application)
  }
}
