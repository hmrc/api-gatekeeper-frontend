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

package uk.gov.hmrc.gatekeeper.models

import java.util.UUID

import play.api.libs.json.{Format, Json}

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.{FieldName, FieldValue}
import uk.gov.hmrc.apiplatform.modules.common.domain.models._

object SubscriptionFields {

  trait Fields {
    val empty = Map.empty[FieldName, FieldValue]
  }

  object Fields extends Fields {
    type Alias = Map[FieldName, FieldValue]
  }

  def fields(tpl: (FieldName, FieldValue)*): Map[FieldName, FieldValue] = Map[FieldName, FieldValue](tpl: _*)

  case class SubscriptionFieldsWrapper(applicationId: ApplicationId, clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr, fields: List[SubscriptionFieldValue])

  case class SubscriptionFieldDefinition(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String)

  case class SubscriptionFieldValue(definition: SubscriptionFieldDefinition, value: FieldValue)

  case class ApplicationApiFieldValues(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr, fieldsId: UUID, fields: Map[FieldName, FieldValue])

  object SubscriptionFieldValue {

    def fromFormValues(name: FieldName, description: String, hint: String, `type`: String, shortDescription: String, value: FieldValue) = {
      SubscriptionFieldValue(SubscriptionFieldDefinition(name, description, hint, `type`, shortDescription), value)
    }

    def toFormValues(subscriptionFieldValue: SubscriptionFieldValue): Option[(FieldName, String, String, String, String, FieldValue)] = {
      Some((
        subscriptionFieldValue.definition.name,
        subscriptionFieldValue.definition.description,
        subscriptionFieldValue.definition.hint,
        subscriptionFieldValue.definition.`type`,
        subscriptionFieldValue.definition.shortDescription,
        subscriptionFieldValue.value
      ))
    }
  }

  case class SubscriptionFieldsPutRequest(clientId: ClientId, apiContext: ApiContext, apiVersion: ApiVersionNbr, fields: Fields.Alias)

  object SubscriptionFieldsPutRequest extends APIDefinitionFormatters {
    implicit val format: Format[SubscriptionFieldsPutRequest] = Json.format[SubscriptionFieldsPutRequest]
  }

  // TODO - Remove Enumeration
  object SubscriptionRedirect extends Enumeration {
    type SubscriptionRedirectType = Value
    val MANAGE_PAGE, APPLICATION_CHECK_PAGE, API_SUBSCRIPTIONS_PAGE = Value
  }

  sealed trait SaveSubscriptionFieldsResponse
  case object SaveSubscriptionFieldsSuccessResponse                                  extends SaveSubscriptionFieldsResponse
  case class SaveSubscriptionFieldsFailureResponse(fieldErrors: Map[String, String]) extends SaveSubscriptionFieldsResponse
}
