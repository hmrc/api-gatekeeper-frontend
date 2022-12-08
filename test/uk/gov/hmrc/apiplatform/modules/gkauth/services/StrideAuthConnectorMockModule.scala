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

package uk.gov.hmrc.apiplatform.modules.gkauth.services

import org.mockito.MockitoSugar
import org.mockito.ArgumentMatchersSugar
import uk.gov.hmrc.apiplatform.modules.gkauth.connectors.StrideAuthConnector
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve._
import uk.gov.hmrc.auth.core.retrieve.Name
import uk.gov.hmrc.auth.core.retrieve.~
import scala.concurrent.Future
import uk.gov.hmrc.auth.core.Enrolment
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.apiplatform.modules.gkauth.config.StrideAuthRoles
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperStrideRole
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.GatekeeperRoles._

trait StrideAuthConnectorMockModule {
  self: MockitoSugar with ArgumentMatchersSugar =>

  val strideAuthRoles: StrideAuthRoles

  protected trait BaseStrideAuthConnectorMock {
    def aMock: StrideAuthConnector

    object Authorise {
      private val defaultName = Name(Some("Bobby"), Some("Example"))
      import strideAuthRoles._

      private lazy val predicateUserRole      = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(USER)
      private lazy val predicateSuperUserRole = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(SUPERUSER)
      private lazy val predicateAdminRole     = StrideAuthorisationPredicateForGatekeeperRole(strideAuthRoles)(ADMIN)

      def returnsFor(userRole: GatekeeperStrideRole, name: Name = defaultName) = userRole match {
        case ADMIN     => returnsAdminEnrolledUserWhenSufficient(name)
        case SUPERUSER => returnsSuperuserEnrolledUserWhenSufficient(name)
        case USER      => returnsUserEnrolledUserWhenSufficient(name)
      }

      def returnsAdminEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(adminRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](*, *)(*, *)).thenReturn(Future.successful(retrievalOk))
      }

      def returnsSuperuserEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(superUserRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateUserRole), *)(*, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateSuperUserRole), *)(*, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdminRole), *)(*, *)).thenReturn(Future.failed(new InsufficientEnrolments))
      }

      def returnsUserEnrolledUserWhenSufficient(name: Name = defaultName) = {
        val retrievalOk: ~[Option[Name], Enrolments] = new ~(Some(name), Enrolments(Set(Enrolment(userRole))))

        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateUserRole), *)(*, *)).thenReturn(Future.successful(retrievalOk))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateSuperUserRole), *)(*, *)).thenReturn(Future.failed(new InsufficientEnrolments))
        when(aMock.authorise[~[Option[Name], Enrolments]](eqTo(predicateAdminRole), *)(*, *)).thenReturn(Future.failed(new InsufficientEnrolments))
      }

      def failsWithNoActiveSession = when(aMock.authorise(*, *)(*, *)).thenReturn(Future.failed(SessionRecordNotFound()))
    }
  }

  object StrideAuthConnectorMock extends BaseStrideAuthConnectorMock {
    val aMock = mock[StrideAuthConnector]
  }
}
