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

package services

import connectors._
import model.SubscriptionFields.{Fields, SaveSubscriptionFieldsSuccessResponse, SubscriptionFieldDefinition, SubscriptionFieldValue}
import model.{APIIdentifier, Application, FieldsDeleteResult}
import org.scalatest.concurrent.ScalaFutures
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.successful

class SubscriptionFieldsServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar with ArgumentMatchersSugar {

  trait Setup {
    val mockSandboxSubscriptionFieldsConnector: SandboxSubscriptionFieldsConnector = mock[SandboxSubscriptionFieldsConnector]
    val mockProductionSubscriptionFieldsConnector: ProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    val underTest: SubscriptionFieldsService = spy(service)
  }

  private val apiIdentifier = APIIdentifier("context", "api-context")

  "When application is deployedTo production then principal connector is called" should {
    val application = mock[Application]
    when(application.clientId).thenReturn("client-id")
    when(application.deployedTo).thenReturn("PRODUCTION")

    "fetchAllFieldDefinitions" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.fetchAllFieldDefinitions()(*))
        .thenReturn(successful(mock[DefinitionsByApiVersion]))

      await (service.fetchAllFieldDefinitions(application.deployedTo))

      verify(mockProductionSubscriptionFieldsConnector).fetchAllFieldDefinitions()(*)
      verify(mockSandboxSubscriptionFieldsConnector, never).fetchAllFieldDefinitions()(*)
    }

    "fetchFieldDefinitions" in new Setup {
      val subscriptionFieldDefinitions = List(
        SubscriptionFieldDefinition("nameOne", "descriptionThree", "hintOne", "typeOne", "shortDescription"),
        SubscriptionFieldDefinition("nameTwo", "descriptionThree", "hintTwo", "typeTwo", "shortDescription"),
        SubscriptionFieldDefinition("nameThree", "descriptionThree", "hintThree", "typeThree", "shortDescription")
      )

      val apiIdentifier = APIIdentifier("testContext", "v1")

      when(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(*, *)(*))
        .thenReturn(subscriptionFieldDefinitions)

      await(service.fetchFieldDefinitions(application.deployedTo, apiIdentifier))

      verify(mockSandboxSubscriptionFieldsConnector, never).fetchFieldDefinitions(*, *)(*)
      verify(mockProductionSubscriptionFieldsConnector).fetchFieldDefinitions(*, *)(*)
    }

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockProductionSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(*, *, *)(*))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiIdentifier, prefetchedDefinitions))

      verify(mockProductionSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiIdentifier), eqTo(prefetchedDefinitions))(*)

      verify(mockSandboxSubscriptionFieldsConnector, never).fetchFieldsValuesWithPrefetchedDefinitions(*,*, *)(*)
    }

    "saveFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.saveFieldValues(*,*,*,*)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiIdentifier.context, apiIdentifier.version, fields))

      verify(mockProductionSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version), eqTo(fields))(*)

      verify(mockSandboxSubscriptionFieldsConnector, never).saveFieldValues(*,*,*,*)(*)
    }

    "deleteFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.deleteFieldValues(*,*,*)(*))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiIdentifier.context, apiIdentifier.version))

      verify(mockProductionSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*)

      verify(mockSandboxSubscriptionFieldsConnector, never).deleteFieldValues(*,*,*)(*)
    }

    "When fetchFieldValues is called" should {

      "return return no field values when given no field definitions" in new Setup {
        private val definitions = Seq.empty

        when(mockProductionSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockProductionSubscriptionFieldsConnector, never)
          .fetchFieldValues(*,*, *)(*)
      }

      "return somme field values when given some field definitions" in new Setup {
        private val definitions = Seq(SubscriptionFieldDefinition("field1","description", "hint", "type", "shortDescription"))

        when(mockProductionSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockProductionSubscriptionFieldsConnector)
          .fetchFieldValues(eqTo(application.clientId),eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*)
      }
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {
    val application = mock[Application]
    when(application.clientId).thenReturn("client-id")
    when(application.deployedTo).thenReturn("SANDBOX")

    "fetchAllFieldDefinitions" in new Setup {
      when(mockSandboxSubscriptionFieldsConnector.fetchAllFieldDefinitions()(*))
        .thenReturn(successful(mock[DefinitionsByApiVersion]))

      await(service.fetchAllFieldDefinitions(application.deployedTo))

      verify(mockSandboxSubscriptionFieldsConnector).fetchAllFieldDefinitions()(*)
      verify(mockProductionSubscriptionFieldsConnector, never).fetchAllFieldDefinitions()(*)
    }

    "fetchFieldDefinitions" in new Setup {
      val subscriptionFieldDefinitions = List(
        SubscriptionFieldDefinition("nameOne", "descriptionThree", "hintOne", "typeOne", "shortDescription"),
        SubscriptionFieldDefinition("nameTwo", "descriptionThree", "hintTwo", "typeTwo", "shortDescription"),
        SubscriptionFieldDefinition("nameThree", "descriptionThree", "hintThree", "typeThree", "shortDescription")
      )

      val apiIdentifier = APIIdentifier("testContext", "v1")

      when(mockSandboxSubscriptionFieldsConnector.fetchFieldDefinitions(*, *)(*))
        .thenReturn(subscriptionFieldDefinitions)

      await(service.fetchFieldDefinitions(application.deployedTo, apiIdentifier))

      verify(mockSandboxSubscriptionFieldsConnector).fetchFieldDefinitions(*, *)(*)
      verify(mockProductionSubscriptionFieldsConnector, never).fetchFieldDefinitions(*, *)(*)
    }

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockSandboxSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(*, *, *)(*))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiIdentifier, prefetchedDefinitions))

      verify(mockSandboxSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiIdentifier), eqTo(prefetchedDefinitions))(*)

      verify(mockProductionSubscriptionFieldsConnector, never).fetchFieldsValuesWithPrefetchedDefinitions(*,*, *)(*)
    }

    "saveFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.saveFieldValues(*,*,*,*)(*))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiIdentifier.context, apiIdentifier.version, fields))

      verify(mockSandboxSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version), eqTo(fields))(*)

      verify(mockProductionSubscriptionFieldsConnector, never).saveFieldValues(*,*,*,*)(*)
    }

    "deleteFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.deleteFieldValues(*,*,*)(*))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiIdentifier.context, apiIdentifier.version))

      verify(mockSandboxSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*)

      verify(mockProductionSubscriptionFieldsConnector, never).deleteFieldValues(*,*,*)(*)
    }

    "When fetchFieldValues is called" should {

      "return return no field values when given no field definitions" in new Setup {
        private val definitions = Seq.empty

        when(mockSandboxSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockSandboxSubscriptionFieldsConnector, never)
          .fetchFieldValues(*,*, *)(*)
      }

      "return somme field values when given some field definitions" in new Setup {
        private val definitions = Seq(SubscriptionFieldDefinition("field1","description", "hint", "type", "shortDescription"))

        when(mockSandboxSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockSandboxSubscriptionFieldsConnector)
          .fetchFieldValues(eqTo(application.clientId),eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(*)
      }
    }
  }
}
