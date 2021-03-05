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

package mocks.service

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import services.DeveloperService
import scala.concurrent.Future.{failed,successful}
import model._
import utils.UserIdTracker
import model.TopicOptionChoice.TopicOptionChoice

trait DeveloperServiceMock {
  self: MockitoSugar with ArgumentMatchersSugar with UserIdTracker =>

  val mockDeveloperService = mock[DeveloperService]

  def aUser(email: String): User = RegisteredUser(email, idOf(email), "first", "last", verified = false)

  def givenSeekUserFindsRegisteredUser(email: String, verified: Boolean = true, mfaEnabled: Boolean = true) = {
    when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(RegisteredUser(email, idOf(email), "first", "last", verified = verified, mfaEnabled = mfaEnabled))))
  }

  def givenSeekUserFindsUnregisteredUser(email: String) = {
    when(mockDeveloperService.seekUser(eqTo(email))(*)).thenReturn(successful(Some(UnregisteredUser(email, idOf(email)))))
  }

  def givenFetchOrCreateUserReturnsRegisteredUser(email: String, verified: Boolean = true, mfaEnabled: Boolean = true): Unit = {
    when(mockDeveloperService.fetchOrCreateUser(eqTo(email))(*)).thenReturn(successful(RegisteredUser(email, idOf(email), "first", "last", verified = verified, mfaEnabled = mfaEnabled)))
  }

  def givenFetchOrCreateUserReturnsUnregisteredUser(email: String): Unit = {
    when(mockDeveloperService.fetchOrCreateUser(eqTo(email))(*)).thenReturn(successful(UnregisteredUser(email, idOf(email))))
  }

  def givenSearchDevelopersFinds(users: User*): Unit =  {
    when(mockDeveloperService.searchDevelopers(any[Developers2Filter])(*)).thenReturn(successful(users.toList))
  }

  def givenUserExists(email: String): Unit = {
    when(mockDeveloperService.fetchUser(eqTo(email))(*)).thenReturn(successful(aUser(email)))
  }

  def givenRegisteredUsers(users: RegisteredUser*): Unit = {
    when(mockDeveloperService.fetchUsers(*)).thenReturn(successful(users.toList))
  }

  def givenFetchDevelopersByEmailPreferences(users: RegisteredUser*) = {
    when(mockDeveloperService.fetchDevelopersByEmailPreferences(*, *)(*)).thenReturn(successful(users.toList))
  }

  def givenFetchDevelopersByAPICategoryEmailPreferences(users: List[RegisteredUser]) = {
    when(mockDeveloperService.fetchDevelopersByAPICategoryEmailPreferences(*[TopicOptionChoice], *[APICategory])(*)).thenReturn(successful(users))
  }

  def givenFetchDevelopersBySpecificAPIEmailPreferences(users: List[RegisteredUser]) = {
    when(mockDeveloperService.fetchDevelopersBySpecificAPIEmailPreferences(*,*, *)(*)).thenReturn(successful(users))
  }


}
