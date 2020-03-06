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
import model.apiSubscriptionFields.{Fields, SubscriptionFieldValue}
import model.{ApiContextVersion, Application, FieldsDeleteResult}
import org.mockito.Matchers.{any, eq => eqTo}
import org.mockito.Mockito.{never, spy, verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import services.SubscriptionFieldsService.DefinitionsByApiVersion
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future.successful

class SubscriptionFieldsServiceSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  trait Setup {
    val mockSandboxSubscriptionFieldsConnector: SandboxSubscriptionFieldsConnector = mock[SandboxSubscriptionFieldsConnector]
    val mockProductionSubscriptionFieldsConnector: ProductionSubscriptionFieldsConnector = mock[ProductionSubscriptionFieldsConnector]

    implicit val hc: HeaderCarrier = HeaderCarrier()

    val service = new SubscriptionFieldsService(mockSandboxSubscriptionFieldsConnector, mockProductionSubscriptionFieldsConnector)
    val underTest: SubscriptionFieldsService = spy(service)
  }

  private val apiContextVersion = ApiContextVersion("context", "api-context")

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

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockProductionSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiContextVersion, prefetchedDefinitions))

      verify(mockProductionSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiContextVersion), eqTo(prefetchedDefinitions))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).fetchFieldsValuesWithPrefetchedDefinitions(any(),any(), any())(any[HeaderCarrier]())
    }

    "saveFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.saveFieldValues(any(),any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[HttpResponse]))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiContextVersion.context, apiContextVersion.version, fields))

      verify(mockProductionSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(application.clientId), eqTo(apiContextVersion.context), eqTo(apiContextVersion.version), eqTo(fields))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).saveFieldValues(any(),any(),any(),any())(any[HeaderCarrier]())
    }

    "deleteFieldValues" in new Setup {

      when(mockProductionSubscriptionFieldsConnector.deleteFieldValues(any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiContextVersion.context, apiContextVersion.version))

      verify(mockProductionSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiContextVersion.context), eqTo(apiContextVersion.version))(any[HeaderCarrier]())

      verify(mockSandboxSubscriptionFieldsConnector, never()).deleteFieldValues(any(),any(),any())(any[HeaderCarrier]())
    }
  }

  "When application is deployed to sandbox then subordinate connector is called" should {

    val application = mock[Application]
    when(application.clientId).thenReturn("client-id")
    when(application.deployedTo).thenReturn("SANDBOX")


    "fetchAllFieldDefinitions" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.fetchAllFieldDefinitions()(any[HeaderCarrier]()))
        .thenReturn(successful(mock[DefinitionsByApiVersion]))

      await (service.fetchAllFieldDefinitions(application.deployedTo))

      verify(mockSandboxSubscriptionFieldsConnector).fetchAllFieldDefinitions()(any[HeaderCarrier]())
      verify(mockProductionSubscriptionFieldsConnector, never()).fetchAllFieldDefinitions()(any[HeaderCarrier]())
    }

    "fetchFieldsWithPrefetchedDefinitions" in new Setup {

      private val prefetchedDefinitions = mock[DefinitionsByApiVersion]

      when(mockSandboxSubscriptionFieldsConnector.fetchFieldsValuesWithPrefetchedDefinitions(any(), any(), any())(any[HeaderCarrier]()))
        .thenReturn(successful(Seq.empty[SubscriptionFieldValue]))

      await (service.fetchFieldsWithPrefetchedDefinitions(application, apiContextVersion, prefetchedDefinitions))

      verify(mockSandboxSubscriptionFieldsConnector)
        .fetchFieldsValuesWithPrefetchedDefinitions(eqTo(application.clientId), eqTo(apiContextVersion), eqTo(prefetchedDefinitions))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).fetchFieldsValuesWithPrefetchedDefinitions(any(),any(), any())(any[HeaderCarrier]())
    }

    "saveFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.saveFieldValues(any(),any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[HttpResponse]))

      val fields: Fields = mock[Fields]

      await (service.saveFieldValues(application, apiContextVersion.context, apiContextVersion.version, fields))

      verify(mockSandboxSubscriptionFieldsConnector)
        .saveFieldValues(eqTo(application.clientId), eqTo(apiContextVersion.context), eqTo(apiContextVersion.version), eqTo(fields))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).saveFieldValues(any(),any(),any(),any())(any[HeaderCarrier]())
    }

    "deleteFieldValues" in new Setup {

      when(mockSandboxSubscriptionFieldsConnector.deleteFieldValues(any(),any(),any())(any[HeaderCarrier]()))
        .thenReturn(successful(mock[FieldsDeleteResult]))

      await (service.deleteFieldValues(application, apiContextVersion.context, apiContextVersion.version))

      verify(mockSandboxSubscriptionFieldsConnector)
        .deleteFieldValues(eqTo(application.clientId), eqTo(apiContextVersion.context), eqTo(apiContextVersion.version))(any[HeaderCarrier]())

      verify(mockProductionSubscriptionFieldsConnector, never()).deleteFieldValues(any(),any(),any())(any[HeaderCarrier]())
    }
  }
}
