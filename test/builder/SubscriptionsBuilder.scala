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
import model.{APIStatus, ApiVersionDefinition, Subscription, VersionSubscription}
import model.{ApiContext, ApiVersion, ClientId, ApplicationId}
import model.FieldName
import model.FieldValue

trait SubscriptionsBuilder {

  def buildSubscription(name: String, context: Option[ApiContext] = None, versions: Seq[VersionSubscription] = Seq.empty) = {
    Subscription(name = name,
      serviceName = s"service-$name",
      context = context.getOrElse(ApiContext(s"context-$name")),
      versions = versions)
  }

  def buildVersionWithSubscriptionFields(version: ApiVersion, subscribed: Boolean, applicationId: ApplicationId, fields: Option[SubscriptionFieldsWrapper] = None) = {
      val defaults = buildSubscriptionFieldsWrapper(applicationId)

      VersionSubscription(ApiVersionDefinition(version, APIStatus.STABLE, None), subscribed = subscribed, fields = fields.getOrElse(defaults))
    }

  def buildSubscriptionFieldsWrapper(applicationId: ApplicationId, fields: Seq[SubscriptionFieldValue] = Seq.empty) = {
    SubscriptionFieldsWrapper(applicationId, ClientId(s"clientId-${applicationId.value}"), ApiContext(s"context-${applicationId.value}"), ApiVersion(s"apiVersion-${applicationId.value}"), fields = fields)
  }


  def buildSubscriptionFieldValue(name: FieldName) = {
    SubscriptionFieldValue(SubscriptionFieldDefinition(name, s"description-${name.value}", s"hint-${name.value}", "STRING", s"shortDescription-${name.value}"), FieldValue(s"value-${name.value}"))
  }
}
