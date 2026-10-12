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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import scala.concurrent.Future

import org.mockito.{ArgumentMatchersSugar, MockitoSugar}

import uk.gov.hmrc.auth.core.retrieve.{Name, ~}
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments, *}

import uk.gov.hmrc.apiplatform.modules.gkauth.config.StrideAuthRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.connectors.StrideAuthConnector
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles.*
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperStrideRole

trait StrideAuthConnectorMockModule {
  self: MockitoSugar & ArgumentMatchersSugar =>

  val strideAuthRoles: StrideAuthRoles

  protected trait BaseStrideAuthConnectorMock {
    def aMock: StrideAuthConnector

    object Authorise {
      private val defaultName = Name(Some("Bobby"), Some("Example"))
      import strideAuthRoles._

      private lazy val predicateUserRole         = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(USER)
      private lazy val predicateSuperUserRole    = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(SUPERUSER)
      private lazy val predicateAdvancedUserRole = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(ADVANCEDUSER)
      private lazy val predicateAdminRole        = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(ADMIN)

      def returnsFor(userRole: GatekeeperStrideRole, name: Name = defaultName) = userRole match {
        case ADMIN        => returnsAdminEnrolledUserWhenSufficient(name)
        case SUPERUSER    => returnsSuperuserEnrolledUserWhenSufficient(name)
        case ADVANCEDUSER => returnsAdvancedUserEnrolledUserWhenSufficient(name)
        case USER         => returnsUserEnrolledUserWhenSufficient(name)
      }

      def returnsAdminEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(adminRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](*, *)(using *, *)).thenReturn(Future.successful(retrievalOk))
      }

      def returnsSuperuserEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(superUserRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateUserRole), *)(using *, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdvancedUserRole), *)(using *, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateSuperUserRole), *)(using *, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdminRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
      }

      def returnsAdvancedUserEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(advancedUserRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateUserRole), *)(using *, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdvancedUserRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateSuperUserRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdminRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
      }

      def returnsUserEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(userRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateUserRole), *)(using *, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdvancedUserRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateSuperUserRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdminRole), *)(using *, *)).thenReturn(Future.failed(new InsufficientEnrolments))
      }

      def failsWithNoActiveSession = when(aMock.authorise(*, *)(using *, *)).thenReturn(Future.failed(SessionRecordNotFound()))
    }
  }

  object StrideAuthConnectorMock extends BaseStrideAuthConnectorMock {
    val aMock = mock[StrideAuthConnector]
  }
}
