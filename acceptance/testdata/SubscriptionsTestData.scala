package testdata

import builder.SubscriptionsBuilder
import model.{ApiContext, ApiVersion}

trait SubscriptionsTestData extends SubscriptionsBuilder {

  val defaultSubscriptions = Set(
     buildApiIdentifier(ApiContext("marriage-allowance"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("api-simulator"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("hello"), ApiVersion("1.0"))
  )
}
