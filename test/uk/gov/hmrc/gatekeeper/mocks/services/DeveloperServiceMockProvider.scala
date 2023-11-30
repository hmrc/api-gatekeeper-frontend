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

package mocks.services

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.Future.{failed, successful}

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.ApiCategory
import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationResponse
import uk.gov.hmrc.apiplatform.modules.common.domain.models.LaxEmailAddress
import uk.gov.hmrc.gatekeeper.models.{TopicOptionChoice, _}
import uk.gov.hmrc.gatekeeper.services.DeveloperService
import uk.gov.hmrc.gatekeeper.utils.UserIdTracker

trait DeveloperServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar with UserIdTracker =>

  val mockDeveloperService = mock[DeveloperService]

  object DeveloperServiceMock {

    val mfaDetail = AuthenticatorAppMfaDetailSummary(MfaId(UUID.randomUUID()), "name", LocalDateTime.now, verified = true)

    def mfaEnabledToMfaDetails(mfaEnabled: Boolean) = {
      if (mfaEnabled) {
        List(mfaDetail)
      } else List.empty
    }

    object FilterUsersBy {

      def returnsFor(apiFilter: ApiFilter[String], apps: ApplicationResponse*)(developers: Developer*) =
        when(mockDeveloperService.filterUsersBy(eqTo(apiFilter), eqTo(apps.toList))(*)).thenReturn(developers.toList)
      def returnsFor(statusFilter: StatusFilter)(developers: Developer*)                               = when(mockDeveloperService.filterUsersBy(eqTo(statusFilter))(*)).thenReturn(developers.toList)
    }

    object GetDevelopersWithApps {

      def returnsFor(apps: ApplicationResponse*)(users: User*)(developers: Developer*) =
        when(mockDeveloperService.getDevelopersWithApps(eqTo(apps.toList), eqTo(users.toList)))
          .thenReturn(developers.toList)
    }

    object FetchUsers {

      def returns(users: RegisteredUser*) =
        when(mockDeveloperService.fetchUsers(*))
          .thenReturn(successful(users.toList))
    }

    object FetchUsersPaginated {

      def returns(totalCount: Int, users: RegisteredUser*) =
        when(mockDeveloperService.fetchUsersPaginated(*, *)(*))
          .thenReturn(successful(UserPaginatedResponse(totalCount, users.toList)))
    }

    object FetchDeveloper {
      def handles(developer: Developer) = when(mockDeveloperService.fetchDeveloper(eqTo(UuidIdentifier(developer.user.userId)), *)(*)).thenReturn(successful(developer))
    }

    object RemoveMfa {
      def returns(user: RegisteredUser) = when(mockDeveloperService.removeMfa(*, *)(*)).thenReturn(successful(user))
      def throws(t: Throwable)          = when(mockDeveloperService.removeMfa(*, *)(*)).thenReturn(failed(t))
    }

    object DeleteDeveloper {

      def returnsFor(developer: Developer, result: DeveloperDeleteResult) =
        when(mockDeveloperService.deleteDeveloper(eqTo(UuidIdentifier(developer.user.userId)), *)(*))
          .thenReturn(successful((result, developer)))
    }

    object FetchDevelopersByEmails {
      def returns(developers: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersByEmails(*)(*)).thenReturn(successful(developers.toList))
    }

    object FetchDevelopersBySpecificTaxRegimesEmailPreferences {

      def returns(developers: RegisteredUser*) =
        when(mockDeveloperService.fetchDevelopersBySpecificTaxRegimesEmailPreferencesPaginated(*, *, *)(*)).thenReturn(successful(UserPaginatedResponse(10, developers.toList)))
    }

    object SearchDevelopers {
      def returns(users: User*) = when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(successful(users.toList))
    }

    object SeekRegisteredUser {

      def returnsFor(email: LaxEmailAddress, verified: Boolean = true, mfaEnabled: Boolean = true) = {

        when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(RegisteredUser(
          email,
          idOf(email),
          "first",
          "last",
          verified = verified,
          mfaEnabled = mfaEnabled,
          mfaDetails = mfaEnabledToMfaDetails(mfaEnabled)
        ))))
      }
    }

    object SeekUnregisteredUser {

      def returnsFor(email: LaxEmailAddress) =
        when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(UnregisteredUser(email, idOf(email)))))
    }

    object FetchOrCreateUser {

      def handles(email: LaxEmailAddress, verified: Boolean = true, mfaEnabled: Boolean = true) =
        when(mockDeveloperService.fetchOrCreateUser(eqTo(email))(*)).thenReturn(successful(RegisteredUser(
          email,
          idOf(email),
          "first",
          "last",
          verified = verified,
          mfaEnabled = mfaEnabled,
          mfaDetails = mfaEnabledToMfaDetails(mfaEnabled)
        )))
    }

    object FetchDevelopersByEmailPreferences {
      def returns(users: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersByEmailPreferences(*, *)(*)).thenReturn(successful(users.toList))
    }

    object FetchDevelopersByEmailPreferencesPaginated {

      def returns(users: RegisteredUser*) =
        when(mockDeveloperService.fetchDevelopersByEmailPreferencesPaginated(*, *, *, *, *, *)(*)).thenReturn(successful(UserPaginatedResponse(users.size, users.toList)))
    }

    object FetchDevelopersBySpecificAPIEmailPreferences {
      def returns(users: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersBySpecificAPIEmailPreferences(*, *, *, *)(*)).thenReturn(successful(users.toList))
    }

    object FetchDevelopersBySpecificApisEmailPreferences {

      def returns(users: RegisteredUser*) =
        when(mockDeveloperService.fetchDevelopersBySpecificApisEmailPreferences(*, *, *)(*)).thenReturn(successful(UserPaginatedResponse(10, users.toList)))
    }

    object FetchDevelopersByAPICategoryEmailPreferences {

      def returns(users: RegisteredUser*) =
        when(mockDeveloperService.fetchDevelopersByAPICategoryEmailPreferences(*[TopicOptionChoice], *[ApiCategory])(*)).thenReturn(successful(users.toList))
    }

    def userExists(email: LaxEmailAddress): Unit = {
      when(mockDeveloperService.fetchUser(eqTo(email))(*)).thenReturn(successful(aUser(email)))
    }
  }

  def aUser(email: LaxEmailAddress, verified: Boolean = false): User = RegisteredUser(email, idOf(email), "first", "last", verified = verified)

}
