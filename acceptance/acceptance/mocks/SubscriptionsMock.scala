package acceptance.mocks

import builder.SubscriptionsBuilder
import model.ApiContext
import model.ApiVersion

trait SubscriptionsMock extends SubscriptionsBuilder {

  val testSubscriptions = Set(
     buildApiIdentifier(ApiContext("marriage-allowance"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("api-simulator"), ApiVersion("1.0")),
     buildApiIdentifier(ApiContext("hello"), ApiVersion("1.0"))
  )
}