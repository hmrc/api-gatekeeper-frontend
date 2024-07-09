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

package mocks.connectors

import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import play.api.http.Status._
import uk.gov.hmrc.http.UpstreamErrorResponse

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.GKApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.connectors.{ApplicationConnector, ProductionApplicationConnector, SandboxApplicationConnector}
import uk.gov.hmrc.gatekeeper.models._

trait ApplicationConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockProductionApplicationConnector = mock[ProductionApplicationConnector]
  val mockSandboxApplicationConnector    = mock[SandboxApplicationConnector]

  trait ApplicationConnectorMock {
    def aMock: ApplicationConnector

    import mocks.PaginatedApplicationResponseBuilder._

    object SearchApplications {
      def returns(apps: GKApplicationResponse*) = when(aMock.searchApplications(*)(*)).thenReturn(successful(buildPaginatedApplicationResponse(apps.toList)))
    }

    object SearchCollaborators {
      def returns(emails: LaxEmailAddress*) = when(aMock.searchCollaborators(*[ApiContext], *[ApiVersionNbr], *)(*)).thenReturn(successful(emails.toList))

      def returnsFor(apiContext: ApiContext, apiVersion: ApiVersionNbr, partialEmailMatch: Option[String])(collaboratorEmails: LaxEmailAddress*) =
        when(aMock.searchCollaborators(eqTo(apiContext), eqTo(apiVersion), eqTo(partialEmailMatch))(*))
          .thenReturn(successful(collaboratorEmails.toList))
    }

    object FetchAllApplications {
      def returns(apps: GKApplicationResponse*) = when(aMock.fetchAllApplications()(*)).thenReturn(successful(apps.toList))
    }

    object FetchAllApplicationsWithStateHistories {

      def returns(applicationStateHistories: ApplicationStateHistory*) = when(aMock.fetchAllApplicationsWithStateHistories()(*))
        .thenReturn(successful(applicationStateHistories.toList))
    }

    object FetchAllApplicationsWithNoSubscriptions {
      def returns(apps: GKApplicationResponse*) = when(aMock.fetchAllApplicationsWithNoSubscriptions()(*)).thenReturn(successful(apps.toList))
    }

    object FetchApplicationsWithSubscriptions {
      def returns(apps: ApplicationWithSubscriptionsResponse*) = when(aMock.fetchApplicationsWithSubscriptions()(*)).thenReturn(successful(apps.toList))
    }

    object FetchAllApplicationsBySubscription {
      def returns(apps: GKApplicationResponse*) = when(aMock.fetchAllApplicationsBySubscription(*, *)(*)).thenReturn(successful(apps.toList))
    }

    object FetchApplication {
      def returns(app: ApplicationWithHistory) = when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(successful(app))

      def failsNotFound() =
        when(aMock.fetchApplication(*[ApplicationId])(*)).thenReturn(failed(UpstreamErrorResponse("Not Found", NOT_FOUND)))
    }

    object FetchApplicationsByUserId {
      def returns(apps: GKApplicationResponse*) = when(aMock.fetchApplicationsByUserId(*[UserId])(*)).thenReturn(successful(apps.toList))
    }

    object FetchApplicationsExcludingDeletedByUserId {
      def returns(apps: GKApplicationResponse*) = when(aMock.fetchApplicationsExcludingDeletedByUserId(*[UserId])(*)).thenReturn(successful(apps.toList))
    }

    object CreatePrivOrROPCApp {
      def returns(result: CreatePrivOrROPCAppResult) = when(aMock.createPrivOrROPCApp(*)(*)).thenReturn(successful(result))
    }
  }

  object ApplicationConnectorMock {

    object Prod extends ApplicationConnectorMock {
      val aMock = mockProductionApplicationConnector
    }

    object Sandbox extends ApplicationConnectorMock {
      val aMock = mockSandboxApplicationConnector
    }
  }
}
