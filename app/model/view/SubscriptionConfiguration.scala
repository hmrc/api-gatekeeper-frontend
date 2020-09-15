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

package model.view

import model.{ApiContext, ApiVersion, FieldName, FieldValue, Subscription, SubscriptionFields, VersionSubscription}
import model.SubscriptionFields.SubscriptionFieldsWrapper
import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}
import model.SubscriptionFields.Fields

case class SubscriptionVersion(apiName: String, apiContext : ApiContext, version: ApiVersion, displayedStatus: String, fields: Seq[SubscriptionField])

object SubscriptionVersion {
  def apply(subscriptionsWithFieldDefinitions: Seq[Subscription]): Seq[SubscriptionVersion] = {
    for {
      sub <- subscriptionsWithFieldDefinitions
      version <- sub.versions
    } yield SubscriptionVersion(sub.name, sub.context, version.version.version, version.version.displayedStatus, SubscriptionField(version.fields))
  }

  def apply(subscription : Subscription, version : VersionSubscription, subscriptionFields : Seq[SubscriptionField]) : SubscriptionVersion = {
    SubscriptionVersion(
      subscription.name,
      subscription.context,
      version.version.version,
      version.version.displayedStatus, 
      subscriptionFields)
  }
}

case class SubscriptionField(name: FieldName, shortDescription: String, description: String, hint: String, value: FieldValue)

object SubscriptionField {
  def apply(fields : SubscriptionFieldsWrapper): Seq[SubscriptionField] = {
    fields.fields.map((field: SubscriptionFields.SubscriptionFieldValue) => {
        SubscriptionField(
          field.definition.name,
          field.definition.shortDescription,
          field.definition.description,
          field.definition.hint,
          field.value)
    })
  }
}

case class SubscriptionFieldValueForm(name: FieldName, value: FieldValue)

case class EditApiMetadataForm(fields: List[SubscriptionFieldValueForm])

object EditApiMetadataForm {
  val form: Form[EditApiMetadataForm] = Form(
    mapping(
      "fields" -> list(
        mapping(
          "name" -> nonEmptyText.transform[FieldName](FieldName(_), fieldName => fieldName.value),
          "value" -> text.transform[FieldValue](FieldValue(_), fieldValue => fieldValue.value)
        )(SubscriptionFieldValueForm.apply)(SubscriptionFieldValueForm.unapply)
      )
    )(EditApiMetadataForm.apply)(EditApiMetadataForm.unapply)
  )

  def toFields(form: EditApiMetadataForm) : Fields.Alias = {
    form.fields
      .map(f => (f.name -> f.value))
      .toMap[FieldName, FieldValue]
  }
}
