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

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}

import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress.StringSyntax
import uk.gov.hmrc.apiplatform.modules.common.domain.models.UserId
import uk.gov.hmrc.gatekeeper.models.RegisteredUser
import uk.gov.hmrc.gatekeeper.pages._
import uk.gov.hmrc.gatekeeper.stubs.{ApiPlatformEventsStub, XmlServicesStub}
import uk.gov.hmrc.gatekeeper.testdata._
import uk.gov.hmrc.gatekeeper.utils.UrlEncoding

class ApiGatekeeperApplicationEventsSpec
    extends ApiGatekeeperBaseSpec
    with StateHistoryTestData
    with ApplicationWithSubscriptionDataTestData
    with ApplicationResponseTestData
    with ApplicationWithStateHistoryTestData
    with XmlServicesStub
    with DisplayEventsTestData
    with DisplayEventTestDataBuilder
    with ApiPlatformEventsStub
    with UrlEncoding {

  val developers = List[RegisteredUser](RegisteredUser("joe.bloggs@example.co.uk".toLaxEmail, UserId.random, "joe", "bloggs", false))

  Feature("Show application changes") {
    Scenario("I see the application changes page with all events displayed") {

      Given("I have successfully logged in to the API Gatekeeper")
      stubPaginatedApplicationList()
      stubApiDefinition()
      signInGatekeeper(app)

      ApplicationsPage.goTo()

      val allEvents = makeSomeEvents()
      stubApplication(applicationWithSubscriptionData.toJsonString, developers, stateHistories.toJsonString, applicationId)
      stubApplicationForActionRefiner(defaultApplicationWithHistory.toJsonString, applicationId)
      stubEvents(applicationId, allEvents)

      When("I select to navigate to the Automated Test Application page")
      ApplicationsPage.clickApplicationNameLink("My new app")

      Then("I am successfully navigated to the Automated Test Application page")
      on(ApplicationPage)

      // TODO...
      When("I select to navigate to the application changes page")
      ApplicationEventsPage(applicationId).goTo()

      Then("I am successfully navigated to the application changes page")
      on(ApplicationEventsPage(applicationId))

      And("The filter is set to ALL")
      ApplicationEventsPage(applicationId).getSelectedEventTag() shouldBe ""
      Thread.sleep(2000)

      And("I can the events")
      verifyCountOfElementsByAttribute("data-event-time", 4)

      verifyEvent(0, event1.eventDateTime, "Collaborator Added", "(GK) iam@admin.com")
      verifyEvent(1, event2.eventDateTime, "Collaborator Added", "iam@admin.com")
      verifyEvent(2, event3.eventDateTime, "Collaborator Removed", "(GK) iam@admin.com")
      verifyEvent(3, event4.eventDateTime, "Subscribed to API", "(GK) iam@admin.com")

      When("I choose the Team members filter")
      stubFilteredEventsByEventTag(applicationId, "TEAM_MEMBER", List(event1, event2, event3))
      ApplicationEventsPage(applicationId).selectEventTag("TEAM_MEMBER")
      ApplicationEventsPage(applicationId).getSelectedActorType() shouldBe ""

      And("I submit the filter")
      ApplicationEventsPage(applicationId).clickSubmit()
      Thread.sleep(2500)

      Then("I can only see the 3 events")
      verifyCountOfElementsByAttribute("data-event-time", 3)

      verifyEvent(0, event1.eventDateTime, "Collaborator Added", "(GK) iam@admin.com")
      verifyEvent(1, event2.eventDateTime, "Collaborator Added", "iam@admin.com")
      verifyEvent(2, event3.eventDateTime, "Collaborator Removed", "(GK) iam@admin.com")

      When("I choose the Actor Type filter")
      stubFilteredEventsByBoth(applicationId, "TEAM_MEMBER", "COLLABORATOR", List(event2))
      ApplicationEventsPage(applicationId).selectActorType("COLLABORATOR")
      ApplicationEventsPage(applicationId).getSelectedActorType() shouldBe "COLLABORATOR"

      And("I submit the filter")
      ApplicationEventsPage(applicationId).clickSubmit()
      Thread.sleep(2500)

      Then("I can only see the 1 events")
      verifyCountOfElementsByAttribute("data-event-time", 1)

      verifyEvent(0, event2.eventDateTime, "Collaborator Added", "iam@admin.com")

    }
  }

  def verifyEvent(counter: Int, expectedDateTime: Instant, expectedTag: String, expectedActor: String) = {
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

    verifyText("data-event-time", dateTimeFormatter.format(expectedDateTime.atZone(ZoneOffset.UTC)), counter)
    verifyText("data-event-tag", expectedTag, counter)
    verifyText("data-event-actor", expectedActor, counter)
  }

}
