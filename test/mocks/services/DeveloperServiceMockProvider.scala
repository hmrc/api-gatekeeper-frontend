/*
 * Copyright 2021 HM Revenue & Customs
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

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import services.DeveloperService
import scala.concurrent.Future.{failed,successful}
import model._
import utils.UserIdTracker
import model.TopicOptionChoice.TopicOptionChoice

trait DeveloperServiceMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar with UserIdTracker =>

  val mockDeveloperService = mock[DeveloperService]

  object DeveloperServiceMock {

    object FilterUsersBy {
      def returnsFor(apiFilter: ApiFilter[String],apps: Application*)(developers: Developer*) = when(mockDeveloperService.filterUsersBy(eqTo(apiFilter), eqTo(apps.toList))(*)).thenReturn(developers.toList)
      def returnsFor(statusFilter: StatusFilter)(developers: Developer*) = when(mockDeveloperService.filterUsersBy(eqTo(statusFilter))(*)).thenReturn(developers.toList)
    }
    object GetDevelopersWithApps {
      def returnsFor(apps: Application*)(users: User*)(developers: Developer*) = 
        when(mockDeveloperService.getDevelopersWithApps(eqTo(apps.toList), eqTo(users.toList)))
        .thenReturn(developers.toList)
    }

    object FetchUsers {
      def returns(users: RegisteredUser*) = 
        when(mockDeveloperService.fetchUsers(*))
        .thenReturn(successful(users.toList))
    }

    object FetchDeveloper {
      def handles(developer: Developer) = when(mockDeveloperService.fetchDeveloper(eqTo(UuidIdentifier(developer.user.userId)))(*)).thenReturn(successful(developer))
    }

    object RemoveMfa {
      def returns(user: RegisteredUser) = when(mockDeveloperService.removeMfa(*, *)(*)).thenReturn(successful(user))
      def throws(t: Throwable) =  when(mockDeveloperService.removeMfa(*, *)(*)).thenReturn(failed(t))
    }
    

    object DeleteDeveloper {
      def returnsFor(developer: Developer, result: DeveloperDeleteResult) =
        when(mockDeveloperService.deleteDeveloper(eqTo(UuidIdentifier(developer.user.userId)), *)(*))
        .thenReturn(successful((result,developer)))
    }
    object FetchDevelopersByEmails {
      def returns(developers: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersByEmails(*)(*)).thenReturn(successful(developers.toList))
    }
    object SearchDevelopers {
      def returns(users: User*) = when(mockDeveloperService.searchDevelopers(*)(*)).thenReturn(successful(users.toList))
    }

    object SeekRegisteredUser {
      def returnsFor(email: String, verified: Boolean = true, mfaEnabled: Boolean = true) =
        when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(RegisteredUser(email, idOf(email), "first", "last", verified = verified, mfaEnabled = mfaEnabled))))
    }
    object SeekUnregisteredUser {
      def returnsFor(email: String) =
        when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(UnregisteredUser(email, idOf(email)))))
    }

    object FetchOrCreateUser {
      def handles(email: String, verified: Boolean = true, mfaEnabled: Boolean = true) =
        when(mockDeveloperService.fetchOrCreateUser(eqTo(email))(*)).thenReturn(successful(RegisteredUser(email, idOf(email), "first", "last", verified = verified, mfaEnabled = mfaEnabled)))
    }

    object FetchDevelopersByEmailPreferences {
      def returns(users: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersByEmailPreferences(*, *)(*)).thenReturn(successful(users.toList))
    }

    object FetchDevelopersBySpecificAPIEmailPreferences {
      def returns(users: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersBySpecificAPIEmailPreferences(*,*, *)(*)).thenReturn(successful(users.toList))
    }

    object FetchDevelopersByAPICategoryEmailPreferences {
      def returns(users: RegisteredUser*) = when(mockDeveloperService.fetchDevelopersByAPICategoryEmailPreferences(*[TopicOptionChoice], *[APICategory])(*)).thenReturn(successful(users.toList))
    }

    def userExists(email: String): Unit = {
      when(mockDeveloperService.fetchUser(eqTo(email))(*)).thenReturn(successful(aUser(email)))
    }
  }
  
  def aUser(email: String, verified: Boolean = false): User = RegisteredUser(email, idOf(email), "first", "last", verified = verified)

}
