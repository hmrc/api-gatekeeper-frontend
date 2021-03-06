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

package mocks.connectors

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import scala.concurrent.Future.successful
import model._
import model.TopicOptionChoice.TopicOptionChoice

import connectors._

trait DeveloperConnectorMockProvider {
  self: MockitoSugar with ArgumentMatchersSugar =>
  
  val mockDeveloperConnector = mock[DeveloperConnector]

  object DeveloperConnectorMock {
    object FetchByEmail {
      def handles(user: User) = when(mockDeveloperConnector.fetchByEmail(eqTo(user.email))(*)).thenReturn(successful(user))
    }

    object FetchByUserId {
      def handles(user: User) = when(mockDeveloperConnector.fetchByUserId(eqTo(user.userId))(*)).thenReturn(successful(user))
    }

    object FetchById {
      def handles(user: User) = {
        when(mockDeveloperConnector.fetchById(eqTo(UuidIdentifier(user.userId)))(*)).thenReturn(successful(user))
        when(mockDeveloperConnector.fetchById(eqTo(EmailIdentifier(user.email)))(*)).thenReturn(successful(user))
      }
    }

    object FetchByEmailPreferences {
      def returnsFor(
        topic: TopicOptionChoice,
        maybeApis: Option[Seq[String]],
        maybeApiCategory: Option[Seq[APICategory]])
        ( users: RegisteredUser*) = 
        when(mockDeveloperConnector.fetchByEmailPreferences(eqTo(topic), eqTo(maybeApis), eqTo(maybeApiCategory))(*)).thenReturn(successful(users.toList))
    }

    object FetchByEmails {
      def returns(users: RegisteredUser*) = when(mockDeveloperConnector.fetchByEmails(*)(*)).thenReturn(successful(users.toList))
      def returnsFor(emails: Set[String])(users: RegisteredUser*) =
        when(mockDeveloperConnector.fetchByEmails(eqTo(emails))(*)).thenReturn(successful(users.toList))
    }

    object SearchDevelopers {
      def returns(users: RegisteredUser*) =
        when(mockDeveloperConnector.searchDevelopers(*,*)(*)).thenReturn(successful(users.toList))

      def returnsFor(maybeEmail: Option[String], status: DeveloperStatusFilter.DeveloperStatusFilter)(users: RegisteredUser*) =
        when(mockDeveloperConnector.searchDevelopers(eqTo(maybeEmail), eqTo(status))(*)).thenReturn(successful(users.toList))

    }

  }
}