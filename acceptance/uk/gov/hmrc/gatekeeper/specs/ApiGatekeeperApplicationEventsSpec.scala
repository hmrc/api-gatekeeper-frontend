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

package uk.gov.hmrc.gatekeeper.specs

import uk.gov.hmrc.apiplatform.modules.developers.domain.models.UserId
import uk.gov.hmrc.gatekeeper.testdata._
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.stubs.XmlServicesStub

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models.EventTags
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax

class ApiGatekeeperApplicationEventsSpec
    extends ApiGatekeeperBaseSpec
    with StateHistoryTestData
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with ApplicationWithStateHistoryTestData
    with ApplicationEventsTestData
    with XmlServicesStub
    with UrlEncoding {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  Feature("Show application changes") {
    Scenario("I see the application changes page with all events displayed") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)

      on(ApplicationsPage)

      val allEvents = makeSomeEvents()
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)
      stubApplicationForActionRefiner(defaultApplicationWithHistory.toJsonString, applicationId)
      stubEvents(applicationId, allEvents)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.selectByApplicationName("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      // TODO...
      When("I select to navigate to the application changes page")
      go(ApplicationEventsPage(applicationId))

      Then("I am successfully navigated to the application changes page")
      on(ApplicationEventsPage(applicationId))

      And("The filter is set to ALL")
      ApplicationEventsPage(applicationId).getTypeOfChange shouldBe ""
      Thread.sleep(2000)

      And("I can the events")
      verifyCountOfElementsByAttribute("data-event-time",4)

      verifyEvent(0, event1.eventDateTime, "Team member added", "AppAdmin-1 with the role ADMIN", "(GK) iam@admin.com")
      verifyEvent(1, event2.eventDateTime, "Team member added", "AppAdmin-2 with the role ADMIN", "(GK) iam@admin.com")
      verifyEvent(2, event3.eventDateTime, "Team member removed", "AppAdmin-2 with the role ADMIN", "(GK) iam@admin.com")
      verifyEvent(3, event4.eventDateTime, "Subscribed to API", "apicontext 1.0", "iam@admin.com")

      When("I choose the Team members filter")
      stubFilteredEvents(applicationId, EventTags.TEAM_MEMBER, List(event1, event2, event3))
      ApplicationEventsPage(applicationId).selectTypeOfChange(EventTags.TEAM_MEMBER)
      ApplicationEventsPage(applicationId).getTypeOfChange shouldBe "Team member"

      And("I submit the filter")
      ApplicationEventsPage(applicationId).submit()
      Thread.sleep(2500)

      Then("I can only see the 3 events")
      verifyCountOfElementsByAttribute("data-event-time",3)

      verifyEvent(0, event1.eventDateTime, "Team member added", "AppAdmin-1 with the role ADMIN", "(GK) iam@admin.com")
      verifyEvent(1, event2.eventDateTime, "Team member added", "AppAdmin-2 with the role ADMIN", "(GK) iam@admin.com")
      verifyEvent(2, event3.eventDateTime, "Team member removed", "AppAdmin-2 with the role ADMIN", "(GK) iam@admin.com")
    }
  }

  def verifyEvent(counter: Int, expectedDateTime: Instant, expectedTag: String, expectedDetails: String, expectedActor: String) = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

    verifyText("data-event-time", dateTimeFormatter.format(expectedDateTime.atZone(ZoneOffset.UTC)), counter)
    verifyText("data-event-tag", expectedTag, counter)
    verifyText("data-event-details", expectedDetails, counter)
    verifyText("data-event-actor", expectedActor, counter) 
  }

}