/*
 * Copyright 2021 HM Revenue & Customs
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

package controllers

import config.AppConfig
import mocks.config.FakeAppConfigImpl
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import utils.AsyncHmrcSpec
import play.api.inject.bind
import uk.gov.hmrc.modules.stride.config.StrideAuthConfig
import uk.gov.hmrc.modules.stride.controllers.actions.ForbiddenHandler

trait ControllerBaseSpec extends AsyncHmrcSpec with GuiceOneAppPerSuite {

  implicit val appConfig: AppConfig = app.injector.instanceOf[AppConfig]
  lazy val strideAuthConfig: StrideAuthConfig = app.injector.instanceOf[StrideAuthConfig]
  lazy val forbiddenHandler = app.injector.instanceOf[ForbiddenHandler]
  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure("metrics.jvm" -> false)
      .overrides(bind[AppConfig].to[FakeAppConfigImpl])
      .build()
}
