/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.gatekeeper.common.controllers.binders

import java.util.UUID

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiCategory, ServiceName}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.*

// Workaround for Play unable to path bind opaque types

object RouteModels {
  case class SimpleApplicationId(value: UUID)  extends AnyVal
  case class SimpleOrganisationId(value: UUID) extends AnyVal
  case class SimpleUserId(value: UUID)         extends AnyVal

  type SimpleClientId      = String
  type SimpleServiceName   = String
  type SimpleApiVersionNbr = String
  type SimpleApiContext    = String
  type SimpleApiCategory   = String

  object ToRouteTypes {

    extension (x: ApplicationId) {
      def simpleApp = SimpleApplicationId(x.value)
    }

    extension (x: UserId) {
      def simpleUser = SimpleUserId(x.value)
    }

    extension (x: OrganisationId) {
      def simpleOrg = SimpleOrganisationId(x.value)
    }

    given Conversion[ServiceName, SimpleServiceName] with
      def apply(x: ServiceName): SimpleServiceName = x.toString

    given Conversion[ApiContext, SimpleApiContext] with
      def apply(x: ApiContext): SimpleApiContext = x.value

    given Conversion[ApiVersionNbr, SimpleApiVersionNbr] with
      def apply(x: ApiVersionNbr): SimpleApiVersionNbr = x.value
  }

  object Conversions {

    given Conversion[SimpleApplicationId, ApplicationId] with
      def apply(x: SimpleApplicationId): ApplicationId = ApplicationId(x.value)

    given Conversion[SimpleOrganisationId, OrganisationId] with
      def apply(x: SimpleOrganisationId): OrganisationId = OrganisationId(x.value)

    given Conversion[SimpleUserId, UserId] with
      def apply(x: SimpleUserId): UserId = UserId(x.value)

    given Conversion[Option[SimpleUserId], Option[UserId]] with
      def apply(ox: Option[SimpleUserId]): Option[UserId] = ox.map(x => UserId.apply(x.value))

    given Conversion[SimpleServiceName, ServiceName] with
      def apply(x: SimpleServiceName): ServiceName = ServiceName(x)

    given Conversion[List[SimpleServiceName], List[ServiceName]] with
      def apply(xs: List[SimpleServiceName]): List[ServiceName] = xs.map(x => ServiceName(x))

    given olsn: Conversion[Option[List[SimpleServiceName]], Option[List[ServiceName]]] with
      def apply(oxs: Option[List[SimpleServiceName]]): Option[List[ServiceName]] = oxs.map(xs => xs.map(x => ServiceName(x)))

    given Conversion[SimpleApiVersionNbr, ApiVersionNbr] with
      def apply(x: SimpleApiVersionNbr): ApiVersionNbr = ApiVersionNbr(x)

    given Conversion[SimpleApiContext, ApiContext] with
      def apply(x: SimpleApiContext): ApiContext = ApiContext(x)

    given Conversion[SimpleApiCategory, ApiCategory] with
      def apply(x: SimpleApiCategory): ApiCategory = ApiCategory.unsafeApply(x)

    given osac: Conversion[Option[SimpleApiCategory], Option[ApiCategory]] with
      def apply(x: Option[SimpleApiCategory]): Option[ApiCategory] = x.flatMap(ApiCategory(_))

  }
}
