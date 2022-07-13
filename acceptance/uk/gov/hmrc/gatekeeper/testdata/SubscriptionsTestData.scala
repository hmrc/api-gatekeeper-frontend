package uk.gov.hmrc.gatekeeper.testdata

import uk.gov.hmrc.gatekeeper.builder.SubscriptionsBuilder
import uk.gov.hmrc.gatekeeper.models.{ApiContext, ApiVersion}

trait SubscriptionsTestData extends SubscriptionsBuilder {

  val defaultSubscriptions = Set(
     buildApiIdentifier(ApiContext("marriage-allowance"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("api-simulator"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("hello"), ApiVersion("1.0"))
  )
}
