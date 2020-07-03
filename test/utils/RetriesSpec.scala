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

package utils

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Scheduler}
import akka.pattern.FutureTimeoutSupport
import config.AppConfig
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.BadRequestException
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

class RetriesSpec extends UnitSpec with ScalaFutures with MockitoSugar {

  trait Setup {
    val mockAppConfig: AppConfig = mock[AppConfig]

    var actualDelay: Option[FiniteDuration] = None
    val mockFutureTimeoutSupport: FutureTimeoutSupport = new FutureTimeoutSupport {
      override def after[T](duration: FiniteDuration, using: Scheduler)(value: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
        actualDelay = Some(duration)
        value
      }
    }

    def underTest = new RetryTestConnector(mockFutureTimeoutSupport, mockAppConfig)
  }

  class RetryTestConnector(val futureTimeout: FutureTimeoutSupport,
                           val appConfig: AppConfig) extends Retries {
    implicit val ec: ExecutionContext = global

    override protected def actorSystem: ActorSystem = ActorSystem("test-actor-system")
  }

  "Retries" should {

    "wait for the configured delay before retrying" in new Setup {
      when(mockAppConfig.retryCount).thenReturn(1)
      private val expectedDelayMilliseconds = Random.nextInt
      private val expectedDelay = FiniteDuration(expectedDelayMilliseconds, TimeUnit.MILLISECONDS)
      when(mockAppConfig.retryDelayMilliseconds).thenReturn(expectedDelayMilliseconds)

      intercept[BadRequestException] {
        await(underTest.retry {
          Future.failed(new BadRequestException(""))
        })
      }
      actualDelay shouldBe Some(expectedDelay)
    }

    "Retry the configured number of times on Bad Request" in new Setup {

      private val expectedRetries = Random.nextInt(3) + 1
      when(mockAppConfig.retryCount).thenReturn(expectedRetries)

      var actualRetries = 0

      private val response: Unit = await(underTest.retry {
        if (actualRetries < expectedRetries) {
          actualRetries += 1
          Future.failed(new BadRequestException(""))
        }
        else Future.successful(())
      })

      response shouldBe ((): Unit)
      actualRetries shouldBe expectedRetries
    }

    "Not retry when retryCount is configured to zero" in new Setup {

      private val expectedRetries = 0
      when(mockAppConfig.retryCount).thenReturn(expectedRetries)
      var actualCalls = 0

      intercept[BadRequestException](
        await(underTest.retry {
          actualCalls += 1
          Future.failed(new BadRequestException(""))
        }))

      actualCalls shouldBe 1
    }

    "Not retry on exceptions other than BadRequestException" in new Setup {

      private val expectedRetries = Random.nextInt(3) + 1
      when(mockAppConfig.retryCount).thenReturn(expectedRetries)
      var actualCalls = 0

      intercept[RuntimeException](
        await(underTest.retry {
          actualCalls += 1
          Future.failed(new RuntimeException(""))
        }))

      actualCalls shouldBe 1
    }
  }
}
