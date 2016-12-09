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

package unit.services

import java.util.UUID

import connectors.ApplicationConnector
import model._
import org.joda.time.DateTime
import org.mockito.ArgumentCaptor
import org.mockito.BDDMockito._
import org.mockito.Matchers.{eq => mEq, _}
import org.scalatest.mock.MockitoSugar
import services.ApplicationService
import uk.gov.hmrc.play.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ApplicationServiceSpec extends UnitSpec with MockitoSugar {

  trait Setup {
    val testApplicationService = new ApplicationService {
      val applicationConnector = mock[ApplicationConnector]
    }
    implicit val hc = HeaderCarrier()

    val collaborators = Set(
      Collaborator("sample@email.com", CollaboratorRole.ADMINISTRATOR),
      Collaborator("someone@email.com", CollaboratorRole.DEVELOPER))

    val allApplications = Seq(
      ApplicationResponse(UUID.randomUUID(),
        "application1", None, collaborators, DateTime.now(), ApplicationState()),
      ApplicationResponse(UUID.randomUUID(),
        "application2", None, collaborators, DateTime.now(), ApplicationState()),
      ApplicationResponse(UUID.randomUUID(),
        "application3", None, collaborators, DateTime.now(), ApplicationState()))
  }

  "applicationService" should {
    
    "list all subscribed applications" in new Setup {
      val allSubcribedApplications = allApplications.map(
          a => SubscribedApplicationResponse(a.id,a.name,a.description,a.collaborators,a.createdOn,a.state,Seq("name")))

      given(testApplicationService.applicationConnector.fetchAllSubscribedApplications()(any[HeaderCarrier])).willReturn(Future.successful(allSubcribedApplications))

      val result = await(testApplicationService.fetchAllSubscribedApplications)
      result shouldEqual allSubcribedApplications
    }
    
    "call applicationConnector with appropriate parameters" in new Setup {
      val applicationId = "applicationId"
      val userName = "userName"
      val appIdCaptor = ArgumentCaptor.forClass(classOf[String])
      val gatekeeperIdCaptor = ArgumentCaptor.forClass(classOf[String])

      given(testApplicationService.applicationConnector.resendVerification(appIdCaptor.capture(), gatekeeperIdCaptor.capture())(any[HeaderCarrier])).willReturn(Future.successful(ResendVerificationSuccessful))

      val result = await(testApplicationService.resendVerification(applicationId, userName))

      appIdCaptor.getValue shouldBe applicationId
      gatekeeperIdCaptor.getValue shouldBe userName
    }

    "list all applications when filtering not provided" in new Setup {
      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier])).willReturn(Future.successful(allApplications))

      val result: Seq[ApplicationResponse] = await(testApplicationService.fetchApplications)
      result shouldEqual allApplications
    }

    "list filtered applications when specific subscription filtering is provided" in new Setup {
      val filteredApplications = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testApplicationService.applicationConnector.fetchAllApplicationsBySubscription(mEq("subscription"))(any[HeaderCarrier])).willReturn(Future.successful(filteredApplications))

      val result = await(testApplicationService.fetchApplications(Value("subscription")))
      result shouldBe filteredApplications
    }

    "list filtered applications when OneOrMoreSubscriptions filtering is provided" in new Setup {
      val noSubscriptions = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      val subscriptions = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application2", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application4", None, collaborators, DateTime.now(), ApplicationState()))

      val allApps = noSubscriptions ++ subscriptions
      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier])).willReturn(Future.successful(allApps))
      given(testApplicationService.applicationConnector.fetchAllApplicationsWithNoSubscriptions()(any[HeaderCarrier])).willReturn(Future.successful(noSubscriptions))
      val result = await(testApplicationService.fetchApplications(OneOrMoreSubscriptions))
      result shouldBe subscriptions
    }

    "list filtered applications when OneOrMoreApplications filtering is provided" in new Setup {
      val allApps = Seq(
        ApplicationResponse(UUID.randomUUID(),
          "application1", None, collaborators, DateTime.now(), ApplicationState()),
        ApplicationResponse(UUID.randomUUID(),
          "application3", None, collaborators, DateTime.now(), ApplicationState()))

      given(testApplicationService.applicationConnector.fetchAllApplications()(any[HeaderCarrier])).willReturn(Future.successful(allApps))
      val result = await(testApplicationService.fetchApplications(OneOrMoreApplications))
      result shouldBe allApps
    }
  }
}
