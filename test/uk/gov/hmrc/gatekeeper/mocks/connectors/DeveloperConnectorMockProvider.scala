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

import scala.concurrent.Future.successful

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.connectors._
import uk.gov.hmrc.gatekeeper.models.{TopicOptionChoice, _}

trait DeveloperConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val mockDeveloperConnector = mock[DeveloperConnector]

  object DeveloperConnectorMock {

    object FetchByEmail {
      def handles(user: AbstractUser) = when(mockDeveloperConnector.fetchByEmail(eqTo(user.email))(*)).thenReturn(successful(user))
    }

    object FetchByUserId {
      def handles(user: AbstractUser) = when(mockDeveloperConnector.fetchByUserId(eqTo(user.userId))(*)).thenReturn(successful(user))
    }

    object RemoveEmailPreferencesByService {
      def returns(response: EmailPreferencesDeleteResult) = when(mockDeveloperConnector.removeEmailPreferencesByService(*)(*)).thenReturn(successful(response))
    }

    object FetchById {

      def handles(user: AbstractUser) = {
        when(mockDeveloperConnector.fetchByUserId(eqTo(user.userId))(*)).thenReturn(successful(user))
      }
    }

    object FetchByEmailPreferences {

      def returnsFor(
          topic: TopicOptionChoice,
          maybeApis: Option[Seq[String]],
          maybeApiCategories: Option[Set[ApiCategory]],
          privateapimatch: Boolean
        )(
          users: RegisteredUser*
        ) =
        when(mockDeveloperConnector.fetchByEmailPreferences(eqTo(topic), eqTo(maybeApis), eqTo(maybeApiCategories), eqTo(privateapimatch))(*))
          .thenReturn(successful(users.toList))
    }

    object FetchByEmailPreferencesPaginated {

      def returnsFor(
          topic: Option[TopicOptionChoice],
          maybeApis: Option[Seq[String]],
          maybeApiCategories: Option[Set[ApiCategory]],
          privateApiMatch: Boolean,
          offset: Int,
          limit: Int
        )(
          users: RegisteredUser*
        ) =
        when(mockDeveloperConnector.fetchByEmailPreferencesPaginated(eqTo(topic), eqTo(maybeApis), eqTo(maybeApiCategories), eqTo(privateApiMatch), eqTo(offset), eqTo(limit))(*))
          .thenReturn(successful(UserPaginatedResponse(users.size, users.toList)))
    }

    object FetchByEmails {
      def returns(users: RegisteredUser*) = when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(users.toList))

      def returnsFor(emails: Set[LaxEmailAddress])(users: RegisteredUser*) =
        when(mockDeveloperConnector.fetchByEmails(eqTo(emails))(*)).thenReturn(successful(users.toList))
    }

    object SearchDevelopers {

      def returns(users: RegisteredUser*) =
        when(mockDeveloperConnector.searchDevelopers(*, *)(*)).thenReturn(successful(users.toList))

      def returnsFor(maybeEmail: Option[String], status: DeveloperStatusFilter.DeveloperStatusFilter)(users: RegisteredUser*) =
        when(mockDeveloperConnector.searchDevelopers(eqTo(maybeEmail), eqTo(status))(*)).thenReturn(successful(users.toList))
    }
  }
}
