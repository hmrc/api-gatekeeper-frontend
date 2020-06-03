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
import model.SubscriptionFields.{Fields, SubscriptionFieldDefinition, SubscriptionFieldValue}
import model.{APIIdentifier, Application, FieldsDeleteResult}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, spy, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future
import scala.concurrent.Future.successful
import model.SubscriptionFields.SaveSubscriptionFieldsSuccessResponse

class SubscriptionFieldsServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

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

      when(mockProductionSubscriptionFieldsConnector.fetchAllFieldDefinitions()(any[HeaderCarrier]()))
        .thenReturn(successful(mock[DefinitionsByApiVersion]))

      await (service.fetchAllFieldDefinitions(application.deployedTo))

      verify(mockProductionSubscriptionFieldsConnector).fetchAllFieldDefinitions()(any[HeaderCarrier]())
      verify(mockSandboxSubscriptionFieldsConnector, never()).fetchAllFieldDefinitions()(any[HeaderCarrier]())
    }

    "fetchFieldDefinitions" in new Setup {
      val subscriptionFieldDefinitions = List(
        SubscriptionFieldDefinition("nameOne", "descriptionThree", "hintOne", "typeOne", "shortDescription"),
        SubscriptionFieldDefinition("nameTwo", "descriptionThree", "hintTwo", "typeTwo", "shortDescription"),
        SubscriptionFieldDefinition("nameThree", "descriptionThree", "hintThree", "typeThree", "shortDescription")
      )

      val apiIdentifier = APIIdentifier("testContext", "v1")

      when(mockProductionSubscriptionFieldsConnector.fetchFieldDefinitions(any(), any())(any[HeaderCarrier]))
        .thenReturn(subscriptionFieldDefinitions)

      await(service.fetchFieldDefinitions(application.deployedTo, apiIdentifier))

      verify(mockSandboxSubscriptionFieldsConnector, never).fetchFieldDefinitions(any(), any())(any[HeaderCarrier])
      verify(mockProductionSubscriptionFieldsConnector).fetchFieldDefinitions(any(), any())(any[HeaderCarrier])
    }

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockProductionSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiIdentifier, prefetchedDefinitions))

      verify(mockProductionSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiIdentifier), eqTo(prefetchedDefinitions))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).fetchFieldsValuesWithPrefetchedDefinitions(any(),any(), any())(any[HeaderCarrier]())
    }

    "saveFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.saveToFieldValues(any(),any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiIdentifier.context, apiIdentifier.version, fields))

      verify(mockProductionSubscriptionFieldsConnector)
        .saveToFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version), eqTo(fields))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).saveToFieldValues(any(),any(),any(),any())(any[HeaderCarrier]())
    }

    "deleteFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.deleteFieldValues(any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiIdentifier.context, apiIdentifier.version))

      verify(mockProductionSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).deleteFieldValues(any(),any(),any())(any[HeaderCarrier]())
    }

    "When fetchFieldValues is called" should {

      "return return no field values when given no field definitions" in new Setup {
        private val definitions = Seq.empty

        when(mockProductionSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockProductionSubscriptionFieldsConnector, never)
          .fetchFieldValues(any(),any(), any())(any[HeaderCarrier])
      }

      "return somme field values when given some field definitions" in new Setup {
        private val definitions = Seq(SubscriptionFieldDefinition("field1","description", "hint", "type", "shortDescription"))

        when(mockProductionSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockProductionSubscriptionFieldsConnector)
          .fetchFieldValues(eqTo(application.clientId),eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier])
      }
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {
    val application = mock[Application]
    when(application.clientId).thenReturn("client-id")
    when(application.deployedTo).thenReturn("SANDBOX")

    "fetchAllFieldDefinitions" in new Setup {
      when(mockSandboxSubscriptionFieldsConnector.fetchAllFieldDefinitions()(any[HeaderCarrier]()))
        .thenReturn(successful(mock[DefinitionsByApiVersion]))

      await(service.fetchAllFieldDefinitions(application.deployedTo))

      verify(mockSandboxSubscriptionFieldsConnector).fetchAllFieldDefinitions()(any[HeaderCarrier]())
      verify(mockProductionSubscriptionFieldsConnector, never()).fetchAllFieldDefinitions()(any[HeaderCarrier]())
    }

    "fetchFieldDefinitions" in new Setup {
      val subscriptionFieldDefinitions = List(
        SubscriptionFieldDefinition("nameOne", "descriptionThree", "hintOne", "typeOne", "shortDescription"),
        SubscriptionFieldDefinition("nameTwo", "descriptionThree", "hintTwo", "typeTwo", "shortDescription"),
        SubscriptionFieldDefinition("nameThree", "descriptionThree", "hintThree", "typeThree", "shortDescription")
      )

      val apiIdentifier = APIIdentifier("testContext", "v1")

      when(mockSandboxSubscriptionFieldsConnector.fetchFieldDefinitions(any(), any())(any[HeaderCarrier]))
        .thenReturn(subscriptionFieldDefinitions)

      await(service.fetchFieldDefinitions(application.deployedTo, apiIdentifier))

      verify(mockSandboxSubscriptionFieldsConnector).fetchFieldDefinitions(any(), any())(any[HeaderCarrier])
      verify(mockProductionSubscriptionFieldsConnector, never).fetchFieldDefinitions(any(), any())(any[HeaderCarrier])
    }

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockSandboxSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiIdentifier, prefetchedDefinitions))

      verify(mockSandboxSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiIdentifier), eqTo(prefetchedDefinitions))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).fetchFieldsValuesWithPrefetchedDefinitions(any(),any(), any())(any[HeaderCarrier]())
    }

    "saveFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.saveToFieldValues(any(),any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(SaveSubscriptionFieldsSuccessResponse))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiIdentifier.context, apiIdentifier.version, fields))

      verify(mockSandboxSubscriptionFieldsConnector)
        .saveToFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version), eqTo(fields))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).saveToFieldValues(any(),any(),any(),any())(any[HeaderCarrier]())
    }

    "deleteFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.deleteFieldValues(any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiIdentifier.context, apiIdentifier.version))

      verify(mockSandboxSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).deleteFieldValues(any(),any(),any())(any[HeaderCarrier]())
    }

    "When fetchFieldValues is called" should {

      "return return no field values when given no field definitions" in new Setup {
        private val definitions = Seq.empty

        when(mockSandboxSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockSandboxSubscriptionFieldsConnector, never)
          .fetchFieldValues(any(),any(), any())(any[HeaderCarrier])
      }

      "return somme field values when given some field definitions" in new Setup {
        private val definitions = Seq(SubscriptionFieldDefinition("field1","description", "hint", "type", "shortDescription"))

        when(mockSandboxSubscriptionFieldsConnector.fetchFieldValues(eqTo(application.clientId), eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier]))
          .thenReturn(Future.successful(Seq.empty))

        await (service.fetchFieldsValues(application, definitions, APIIdentifier(apiIdentifier.context, apiIdentifier.version)))

        verify(mockSandboxSubscriptionFieldsConnector)
          .fetchFieldValues(eqTo(application.clientId),eqTo(apiIdentifier.context), eqTo(apiIdentifier.version))(any[HeaderCarrier])
      }
    }
  }
}
