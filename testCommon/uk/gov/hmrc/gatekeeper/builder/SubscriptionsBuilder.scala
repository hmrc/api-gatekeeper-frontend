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

package uk.gov.hmrc.gatekeeper.builder

import scala.util.Random

import uk.gov.hmrc.apiplatform.modules.apis.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{SubscriptionFieldDefinition, SubscriptionFieldValue, SubscriptionFieldsWrapper}
import uk.gov.hmrc.gatekeeper.models.{ApiStatus, ApiVersionGK, Subscription, VersionSubscription, _}

trait SubscriptionsBuilder {

  def buildSubscription(name: String, context: Option[ApiContext] = None, versions: List[VersionSubscription] = List.empty) = {
    Subscription(name = name, serviceName = s"service-$name", context = context.getOrElse(ApiContext(s"context-$name")), versions = versions)
  }

  def buildApiIdentifier(apiContext: ApiContext, apiVersion: ApiVersion): ApiIdentifier = ApiIdentifier(apiContext, apiVersion)

  def buildVersionWithSubscriptionFields(version: ApiVersion, subscribed: Boolean, applicationId: ApplicationId, fields: Option[SubscriptionFieldsWrapper] = None) = {
    val defaults = buildSubscriptionFieldsWrapper(applicationId)

    VersionSubscription(ApiVersionGK(version, ApiVersionSource.UNKNOWN, ApiStatus.STABLE, None), subscribed = subscribed, fields = fields.getOrElse(defaults))
  }

  def buildSubscriptionFieldsWrapper(applicationId: ApplicationId, fields: List[SubscriptionFieldValue] = List.empty) = {
    val text = applicationId.value.toString()
    SubscriptionFieldsWrapper(applicationId, ClientId(s"clientId-$text"), ApiContext(s"context-$text"), ApiVersion(s"apiVersion-$text"), fields = fields)
  }

  def buildSubscriptionFieldDefinition(
      fieldName: FieldName = FieldName.random,
      description: String = Random.nextString(8),
      hint: String = Random.nextString(8),
      `type`: String = "STRING",
      shortDescription: String = Random.nextString(8)
    ) = SubscriptionFieldDefinition(fieldName, description, hint, `type`, shortDescription)

  def buildSubscriptionFieldValue(name: FieldName) = {
    SubscriptionFieldValue(
      buildSubscriptionFieldDefinition(name, s"description-${name.value}", s"hint-${name.value}", "STRING", s"shortDescription-${name.value}"),
      FieldValue(s"value-${name.value}")
    )
  }
}
