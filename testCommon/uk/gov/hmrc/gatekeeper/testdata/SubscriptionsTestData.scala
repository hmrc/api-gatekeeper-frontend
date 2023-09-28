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

package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.SubscriptionsBuilder
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

trait SubscriptionsTestData extends SubscriptionsBuilder {

  val defaultSubscriptions = Set(
    buildApiIdentifier(ApiContext("marriage-allowance"), ApiVersionNbr("1.0")),
    buildApiIdentifier(ApiContext("api-simulator"), ApiVersionNbr("1.0")),
    buildApiIdentifier(ApiContext("hello"), ApiVersionNbr("1.0"))
  )
}
