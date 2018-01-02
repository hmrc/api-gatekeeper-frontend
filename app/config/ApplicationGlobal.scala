/*
 * Copyright 2018 HM Revenue & Customs
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

package config

import com.typesafe.config.Config
import controllers.routes
import net.ceedubs.ficus.Ficus._
import org.joda.time.Duration
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.mvc.{Call, EssentialFilter, Request, RequestHeader}
import play.api.{Application, Play}
import play.twirl.api.Html
import uk.gov.hmrc.crypto.ApplicationCrypto
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.config.{AppName, ControllerConfig, RunMode}
import uk.gov.hmrc.play.frontend.bootstrap.DefaultFrontendGlobal
import uk.gov.hmrc.play.frontend.filters.SessionCookieCryptoFilter
import uk.gov.hmrc.play.frontend.config.LoadAuditingConfig
import uk.gov.hmrc.play.frontend.filters._

object ApplicationGlobal extends DefaultFrontendGlobal with RunMode {

  override val auditConnector = ApplicationAuditConnector
  override val frontendAuditFilter = ApplicationAuditFilter
  override val loggingFilter = LoggingFilter
  implicit lazy val appConfig = AppConfig

  override def onStart(app: Application) {
    super.onStart(app)
    ApplicationCrypto.verifyConfiguration()
  }

  override def microserviceMetricsConfig(implicit app: Application) =
    app.configuration.getConfig(s"$env.microservice.metrics")

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit rh: Request[_]): Html =
  views.html.error_template(pageTitle, heading, message)

  override def sessionTimeoutFilter: SessionTimeoutFilter = {
    val defaultTimeout = Duration.standardMinutes(15)
    val timeoutDuration = configuration
      .getLong("session.timeoutSeconds")
      .map(Duration.standardSeconds)
      .getOrElse(defaultTimeout)

    val wipeIdleSession = configuration
      .getBoolean("session.wipeIdleSession")
      .getOrElse(true)

    val additionalSessionKeysToKeep = configuration
      .getStringSeq("session.additionalSessionKeysToKeep")
      .getOrElse(Seq.empty).toSet

    val whitelistedCalls = Set(routes.AccountController.loginPage()) map { call => WhitelistedCall(call.url, call.method) }

    new SessionTimeoutFilterWithWhitelist(
      timeoutDuration = timeoutDuration,
      additionalSessionKeysToKeep = additionalSessionKeysToKeep,
      onlyWipeAuthToken = !wipeIdleSession,
      whitelistedCalls = whitelistedCalls
    )
  }
}

object ControllerConfiguration extends ControllerConfig {
  lazy val controllerConfigs = Play.current.configuration.underlying.as[Config]("controllers")
}

object LoggingFilter extends FrontendLoggingFilter with MicroserviceFilterSupport {
  override def controllerNeedsLogging(controllerName: String): Boolean =
    ControllerConfiguration.paramsForController(controllerName).needsLogging
}

object ApplicationAuditFilter extends FrontendAuditFilter with MicroserviceFilterSupport with RunMode with AppName {

  override lazy val maskedFormFields = Seq("password")
  override lazy val applicationPort = None
  override lazy val auditConnector = ApplicationAuditConnector

  override def controllerNeedsAuditing(controllerName: String) =
    ControllerConfiguration.paramsForController(controllerName).needsAuditing
}

object ApplicationAuditConnector extends AuditConnector with AppName with RunMode {
  override lazy val auditingConfig = LoadAuditingConfig(s"$env.auditing")
}