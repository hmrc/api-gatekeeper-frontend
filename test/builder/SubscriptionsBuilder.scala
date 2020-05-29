/*
 * Copyright 2020 HM Revenue & Customs
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

package builder

import model.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}
import model.{APIStatus, APIVersion, Subscription, VersionSubscription}

trait SubscriptionsBuilder {

  def buildSubscription(name: String, context: Option[String] = None, versions: Seq[VersionSubscription] = Seq.empty) = {
    Subscription(name = name,
      serviceName = s"service-$name",
      context =  context.getOrElse(s"context-$name"),
      versions = versions)
  }

  def buildVersionWithSubscriptionFields(version: String, subscribed: Boolean, fields: Option[SubscriptionFieldsWrapper] = None) =
    {
      VersionSubscription(APIVersion(version, APIStatus.STABLE, None), subscribed = subscribed, fields = fields)
    }

  def buildSubscriptionFieldsWrapper(applicationId: String, fields: Seq[SubscriptionFieldValue] = Seq.empty) = {
    SubscriptionFieldsWrapper(applicationId, s"clientId-$applicationId", s"context-$applicationId", s"apiVersion-$applicationId", fields = fields)
  }


  def buildSubscriptionFieldValue(name: String) = {
    SubscriptionFieldValue(SubscriptionFieldDefinition(name, s"description-$name", s"hint-$name", "STRING", s"shortDescription-$name"), s"value-$name")
  }
}
