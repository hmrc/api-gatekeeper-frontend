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

package uk.gov.hmrc.gatekeeper.models

import play.api.libs.json.{Format, Json}
import scala.util.Random
import java.util.UUID

case class FieldName(value: String) extends AnyVal

object FieldName {
  implicit val ordering: Ordering[FieldName] = new Ordering[FieldName] {
    override def compare(x: FieldName, y: FieldName): Int = x.value.compareTo(y.value)
  }

  def random = FieldName(Random.alphanumeric.take(8).mkString)
}

case class FieldValue(value: String) extends AnyVal {
  def isEmpty = value.isEmpty
}

object FieldValue {
  def empty = FieldValue("")

  def random = FieldValue(Random.alphanumeric.take(8).mkString)
}

object SubscriptionFields {
  trait Fields {
    val empty = Map.empty[FieldName, FieldValue]
  }

  object Fields extends Fields {
    type Alias = Map[FieldName,FieldValue]
  }

  def fields(tpl: (FieldName, FieldValue)*): Map[FieldName, FieldValue] = Map[FieldName, FieldValue](tpl: _*)

  case class SubscriptionFieldsWrapper(applicationId: ApplicationId, clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fields: List[SubscriptionFieldValue])

  case class SubscriptionFieldDefinition(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String)

  case class SubscriptionFieldValue(definition : SubscriptionFieldDefinition, value: FieldValue)

  case class ApplicationApiFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fieldsId: UUID, fields: Map[FieldName, FieldValue])

  object SubscriptionFieldValue {
    def fromFormValues(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String, value: FieldValue) = {
        SubscriptionFieldValue(SubscriptionFieldDefinition(name, description, hint, `type`, shortDescription), value)
    }

    def toFormValues(subscriptionFieldValue: SubscriptionFieldValue): Option[(FieldName, String, String, String, String, FieldValue)] = {
      Some((subscriptionFieldValue.definition.name,
        subscriptionFieldValue.definition.description,
        subscriptionFieldValue.definition.hint,
        subscriptionFieldValue.definition.`type`,
        subscriptionFieldValue.definition.shortDescription,
        subscriptionFieldValue.value))
    }
  }

  case class SubscriptionFieldsPutRequest(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersion, fields: Fields.Alias)
  object SubscriptionFieldsPutRequest extends APIDefinitionFormatters {
    implicit val format: Format[SubscriptionFieldsPutRequest] = Json.format[SubscriptionFieldsPutRequest]
  }

  object SubscriptionRedirect extends Enumeration {
    type SubscriptionRedirectType = Value
    val MANAGE_PAGE, APPLICATION_CHECK_PAGE, API_SUBSCRIPTIONS_PAGE = Value
  }

  sealed trait SaveSubscriptionFieldsResponse
  case object SaveSubscriptionFieldsSuccessResponse extends SaveSubscriptionFieldsResponse
  case class SaveSubscriptionFieldsFailureResponse(fieldErrors : Map[String, String]) extends SaveSubscriptionFieldsResponse
}


