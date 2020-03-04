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

package model

import java.util.UUID

import model.Forms.SubscriptionFieldsForm
import play.api.data.Form
import play.api.libs.json.{Format, Json}

package object apiSubscriptionFields {

  type Fields = Map[String, String]

  def fields(tpl: (String, String)*): Map[String, String] = Map[String, String](tpl: _*)

  case class SubscriptionFieldsWrapper(applicationId: String, clientId: String, apiContext: String, apiVersion: String, fields: Seq[SubscriptionFieldValue])

  case class SubscriptionFieldDefinition(name: String, description: String, hint: String, `type`: String)

  // TODO: Should the value be an option type? Or an empty string? (I think the latter)
  case class SubscriptionFieldValue(name: String, description: String, hint: String, `type`: String, value: Option[String])

  object SubscriptionFieldValue {

    // TODO: Should this take an option or a string for the value
    def apply(definition: SubscriptionFieldDefinition, value: Option[String]): SubscriptionFieldValue = {
      new SubscriptionFieldValue(definition.name, definition.description, definition.hint,definition.`type`, value)
    }
  }


  case class SubscriptionFieldsPutRequest(clientId: String, apiContext: String, apiVersion: String, fields: Map[String, String])
  object SubscriptionFieldsPutRequest {
    implicit val format: Format[SubscriptionFieldsPutRequest] = Json.format[SubscriptionFieldsPutRequest]
  }

  object SubscriptionRedirect extends Enumeration {
    type SubscriptionRedirectType = Value
    val MANAGE_PAGE, APPLICATION_CHECK_PAGE, API_SUBSCRIPTIONS_PAGE = Value
  }

  case class SubscriptionFieldsViewModel(applicationId: String, apiContext: String, apiVersion: String, subFieldsForm: Form[SubscriptionFieldsForm])

  // TODO: Remove 'response' from name (as used as a general DTO)
  // TODO: Change SubscriptionField to definition (is this only used for definition and not values?)
  case class FieldDefinitionsResponse(fieldDefinitions: List[SubscriptionFieldDefinition])
  object FieldDefinitionsResponse {
    import APIDefinition._
    implicit val formatFieldDefinitionsResponse: Format[FieldDefinitionsResponse] = Json.format[FieldDefinitionsResponse]
  }

  // TODO: Test me?
  case class AllFieldDefinitionsResponse(apis: FieldDefinitionsResponse)
  object AllFieldDefinitionsResponse {
    import APIDefinition._
    implicit val formatAllFieldDefinitionsResponse: Format[AllFieldDefinitionsResponse] = Json.format[AllFieldDefinitionsResponse]
  }

}
