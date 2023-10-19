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

package uk.gov.hmrc.gatekeeper.models.view

import play.api.data.Form
import play.api.data.Forms.{nonEmptyText, _}

import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.{Fields, SubscriptionFieldDefinition}
import uk.gov.hmrc.gatekeeper.models.{ApplicationWithSubscriptionDataAndFieldDefinitions, _}

case class SubscriptionVersion(apiName: String, apiContext: ApiContext, versionNbr: ApiVersionNbr, displayedStatus: String, fields: List[SubscriptionField])

object SubscriptionVersion {

  def apply(app: ApplicationWithSubscriptionDataAndFieldDefinitions): List[SubscriptionVersion] = {
    app.apiDefinitionFields.flatMap(contextMap => {
      contextMap._2.map(versionMap => {
        def toSubscriptionFields(fieldNames: Map[FieldName, SubscriptionFieldDefinition]): List[SubscriptionField] = {
          fieldNames.map(fieldName => {
            val subscriptionFieldDefinition = fieldName._2
            val fieldValue                  = app.applicationWithSubscriptionData.subscriptionFieldValues(contextMap._1)(versionMap._1)(fieldName._1)

            SubscriptionField(fieldName._1, subscriptionFieldDefinition.shortDescription, subscriptionFieldDefinition.description, subscriptionFieldDefinition.hint, fieldValue)
          }).toList
        }

        val apiDefinition = app.allPossibleSubs.find(_.context == contextMap._1).get

        SubscriptionVersion(
          apiDefinition.name,
          contextMap._1,
          versionMap._1,
          apiDefinition.versions(versionMap._1).status.displayText,
          toSubscriptionFields(versionMap._2)
        )
      })
    }).toList
  }
}

case class SubscriptionField(name: FieldName, shortDescription: String, description: String, hint: String, value: FieldValue)

case class SubscriptionFieldValueForm(name: FieldName, value: FieldValue)

case class EditApiMetadataForm(fields: List[SubscriptionFieldValueForm])

object EditApiMetadataForm {

  val form: Form[EditApiMetadataForm] = Form(
    mapping(
      "fields" -> list(
        mapping(
          "name"  -> nonEmptyText.transform[FieldName](FieldName(_), fieldName => fieldName.value),
          "value" -> text.transform[FieldValue](FieldValue(_), fieldValue => fieldValue.value)
        )(SubscriptionFieldValueForm.apply)(SubscriptionFieldValueForm.unapply)
      )
    )(EditApiMetadataForm.apply)(EditApiMetadataForm.unapply)
  )

  def toFields(form: EditApiMetadataForm): Fields.Alias = {
    form.fields
      .map(f => (f.name -> f.value))
      .toMap[FieldName, FieldValue]
  }
}
