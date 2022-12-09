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

package uk.gov.hmrc.gatekeeper.specs

import uk.gov.hmrc.gatekeeper.testdata._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.models.UserId
import uk.gov.hmrc.gatekeeper.stubs.XmlServicesStub

class ApiGatekeeperApplicationEventsSpec
    extends ApiGatekeeperBaseSpec 
    with StateHistoryTestData 
    with ApplicationWithSubscriptionDataTestData 
    with ApplicationResponseTestData 
    with ApplicationWithStateHistoryTestData
    with ApplicationEventsTestData
    with XmlServicesStub
    with UrlEncoding {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk", UserId.random, "joe", "bloggs", false))

  Feature("Show application changes") {
    Scenario("I see the application changes page") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)

      on(ApplicationsPage)

      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId, sampleEvents(applicationId))
      stubApplicationForActionRefiner(defaultApplicationWithHistory.toJsonString, applicationId)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      // TODO...
      go(ApplicationEventsPage(applicationId))
      
      When("I select to navigate to the application changes page")
      on(ApplicationEventsPage(applicationId))

    }
  }
}
