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

package uk.gov.hmrc.gatekeeper.builder

import uk.gov.hmrc.apiplatform.modules.apis.domain.models.{ApiContext, ApiVersion}
import uk.gov.hmrc.gatekeeper.models.ApiDefinitions
import uk.gov.hmrc.gatekeeper.models.FieldName
import uk.gov.hmrc.gatekeeper.models.SubscriptionFields.SubscriptionFieldDefinition

trait FieldDefinitionsBuilder {

  def buildApiDefinitions(): ApiDefinitions.Alias = {
    def contexts = Seq.fill(1)(ApiContext.random)
    buildApiContexts(contexts)
  }

  private def buildApiContexts(apiContexts: Seq[ApiContext]): ApiDefinitions.Alias = {
    def versions = Seq.fill(2)(ApiVersion.random)
    apiContexts.map(apiContext => (apiContext -> buildVersions(versions))).toMap
  }

  private def buildVersions(apiVersions: Seq[ApiVersion]): Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]] = {
    def fieldNames = Seq.fill(2)(FieldName.random)
    apiVersions.map(apiVersion => (apiVersion -> buildFields(fieldNames))).toMap
  }

  private def buildFields(fieldNames: Seq[FieldName]): Map[FieldName, SubscriptionFieldDefinition] = {
    fieldNames.map(fieldName => (fieldName -> buildSubscriptionFieldDefinition(fieldName))).toMap
  }

  private def buildSubscriptionFieldDefinition(fieldName: FieldName): SubscriptionFieldDefinition = {
    SubscriptionFieldDefinition(fieldName, "Description", "This is a hint", "URL", "shortDescription")
  }
}
