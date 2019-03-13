/*
 * Copyright 2019 HM Revenue & Customs
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

package unit.utils

import model._
import org.joda.time.DateTime
import uk.gov.hmrc.play.test.UnitSpec
import utils.SubscriptionEnhancer


class SubscriptionEnhancerSpec extends UnitSpec {

  "SubscriptionEnhancer" when {

    val subscriptionEnhancer = SubscriptionEnhancer

    "Combining SubscribedApplicationResponses with ApiDefinitions" should {

      val appResponses = Seq(
        createApplicationResponse("Hello World App", Seq(SubscriptionNameAndVersion("hello","1.0"))),
        createApplicationResponse("Employers App", Seq(SubscriptionNameAndVersion("employers-paye","1.0"))),
        createApplicationResponse("Unkown Subscription App", Seq(SubscriptionNameAndVersion("unknown","1.0")))
      )

      val apiDefinitions = Seq(createDefinition("Hello World", "hello"),
        createDefinition("Employers PAYE", "employers-paye"))

      val result: Seq[DetailedSubscribedApplicationResponse] = subscriptionEnhancer.combine(appResponses, apiDefinitions)

      "Retain all subscriptions" in {
        result should have size appResponses.size
      }

      "Replace any subscription contexts with SubscriptionDetails" in {
        result.head.subscriptions.exists(_.name == "Hello World") should be(true)
        result.tail.head.subscriptions.exists(_.name == "Employers PAYE") should be(true)
      }

      "Retain the context information" in {
        result.head.subscriptions.exists(_.context == "hello") should be(true)
        result.tail.head.subscriptions.exists(_.context == "employers-paye") should be(true)
      }

      "Make no changes to the subscription details if no definition can be found" in {
        result.tail.tail.head.subscriptions.head.name should be("unknown")
        result.tail.tail.head.subscriptions.head.context should be("unknown")
      }

      "Handle combining no application responses gracefully" in {
        val result = subscriptionEnhancer.combine(Seq.empty, apiDefinitions)
        result shouldBe empty
      }
    }
  }

  def createApplicationResponse(name:String, subs:Seq[SubscriptionNameAndVersion]): SubscribedApplicationResponse =
    SubscribedApplicationResponse(java.util.UUID.randomUUID(), name, Some("Description"),
      Set.empty, DateTime.now(), ApplicationState(), Standard(), subs, termsOfUseAgreed = true, deployedTo = "PRODUCTION")

  def createDefinition(name: String, context:String) = {
    APIDefinition("TestService", "localhost", name, "Test Description", context,
      Seq(APIVersion("1.0", APIStatus.STABLE, None)), None)
  }
}
