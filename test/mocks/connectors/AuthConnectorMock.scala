/*
 * Copyright 2022 HM Revenue & Customs
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
import scala.concurrent.Future.{failed, successful}
import uk.gov.hmrc.modules.stride.connectors.AuthConnector
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, InsufficientEnrolments, InvalidBearerToken}
import uk.gov.hmrc.auth.core.retrieve.{~, Name, Retrieval}

trait AuthConnectorMock {
  self: MockitoSugar with ArgumentMatchersSugar =>

  import mocks.TestRoles._
  
  val userName = "userName"
  val superUserName = "superUserName"
  val adminName = "adminName"

  val mockAuthConnector = mock[AuthConnector]

    def givenAUnsuccessfulLogin(): Unit = {
    when(mockAuthConnector.authorise(*, *)(*, *)).thenReturn(failed(new InvalidBearerToken))
  }

  def givenTheGKUserIsAuthorisedAndIsANormalUser(): Unit = {
    val response = successful(new ~(Some(Name(Some(userName), None)), Enrolments(Set(Enrolment(userRole)))))

    when(mockAuthConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
      .thenReturn(response)
  }

  def givenTheGKUserHasInsufficientEnrolments(): Unit = {
    when(mockAuthConnector.authorise(*, *[Retrieval[Any]])(*, *))
      .thenReturn(failed(new InsufficientEnrolments))
  }

  def givenTheGKUserIsAuthorisedAndIsASuperUser(): Unit = {
    val response = successful(new ~(Some(Name(Some(superUserName), None)), Enrolments(Set(Enrolment(superUserRole)))))

    when(mockAuthConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
      .thenReturn(response)
  }

  def givenTheGKUserIsAuthorisedAndIsAnAdmin(): Unit = {
    val response = successful(new ~(Some(Name(Some(adminName), None)), Enrolments(Set(Enrolment(adminRole)))))

    when(mockAuthConnector.authorise(*, any[Retrieval[~[Option[Name], Enrolments]]])(*, *))
      .thenReturn(response)
  }

  def verifyAuthConnectorCalledForUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole) or Enrolment(userRole)), any[Retrieval[~[Option[Name], Enrolments]]])(*, *)
  }

  def verifyAuthConnectorCalledForSuperUser = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole) or Enrolment(superUserRole)), any[Retrieval[~[Option[Name], Enrolments]]])(*, *)
  }

  def verifyAuthConnectorCalledForAdmin = {
    verify(mockAuthConnector)
      .authorise(eqTo(Enrolment(adminRole)), any[Retrieval[~[Option[Name], Enrolments]]])(*, *)
  }
}
