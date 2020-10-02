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

import model.ApiContext
import model.ApiDefinitions
import model.ApiVersion
import model.FieldName
import model.SubscriptionFields.SubscriptionFieldDefinition
import scala.util.Random

trait FieldDefinitionsBuilder {
  
  def buildApiDefinitions() : ApiDefinitions.Alias = {
    def contexts = gen10[ApiContext](ApiContext.random)
    buildApiContexts(contexts:_*)
  }

  private def buildApiContexts(apiContexts: ApiContext*) : ApiDefinitions.Alias = {
    def versions = gen10[ApiVersion](ApiVersion.random)
    apiContexts.map(apiContext => (apiContext -> buildVersions(versions:_*))).toMap
  }

  private def buildVersions(apiVersions: ApiVersion*) : Map[ApiVersion, Map[FieldName, SubscriptionFieldDefinition]] = {
    def fieldNames = gen10[FieldName](FieldName.random)
    apiVersions.map(apiVersion => (apiVersion -> buildFields(fieldNames:_*))).toMap
  }

  private def buildFields(fieldNames: FieldName* ) : Map[FieldName, SubscriptionFieldDefinition] = {
    fieldNames.map(fieldName => (fieldName -> buildSubscriptionFieldDefinition(fieldName))).toMap
  }

  private def buildSubscriptionFieldDefinition(fieldName: FieldName) : SubscriptionFieldDefinition = {
    SubscriptionFieldDefinition(fieldName, "Description", "This is a hint", "URL", "shortDescription")
  }

  private def gen[T](maxSize: Int)(f: => T) = for(i <- 1 to Random.nextInt(maxSize)) yield f

  private def gen10[T](f: => T) = gen(10)(f)
}