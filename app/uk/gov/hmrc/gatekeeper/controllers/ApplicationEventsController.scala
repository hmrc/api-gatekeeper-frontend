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

package uk.gov.hmrc.gatekeeper.controllers

import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import scala.concurrent.{ExecutionContext, Future}

import com.google.inject.{Inject, Singleton}

import play.api.data.Form
import play.api.mvc._

import uk.gov.hmrc.apiplatform.modules.applications.core.domain.models.ApplicationName
import uk.gov.hmrc.apiplatform.modules.common.domain.models._
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.events.connectors.{DisplayEvent, EnvironmentAwareApiPlatformEventsConnector, QueryableValues}
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.gatekeeper.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.gatekeeper.services.{ApmService, ApplicationQueryService}
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.gatekeeper.views.html.applications._

object ApplicationEventsController {
  case class EventModel(eventDateTime: String, eventTag: String, eventDetails: List[String], actor: String)

  object EventModel {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

    def apply(event: DisplayEvent): EventModel = {
      def applicationEventWho(actor: Actor): String = actor match {
        case Actors.AppCollaborator(email) => email.text
        case Actors.GatekeeperUser(user)   => s"(GK) $user"
        case Actors.ScheduledJob(jobId)    => s"Job($jobId)"
        case Actors.Process(processName)   => s"Process($processName)"
        case Actors.Unknown                => "Unknown"
      }
      EventModel(
        event.eventDateTime.atZone(ZoneOffset.UTC).format(dateTimeFormatter),
        event.eventType,
        event.metaData,
        applicationEventWho(event.actor)
      )
    }
  }
  case class QueryModel(applicationId: ApplicationId, applicationName: ApplicationName, queryableValues: QueryableValues, events: Seq[EventModel])

  case class QueryForm(eventTag: Option[String], actorType: Option[String])

  object QueryForm {
    import play.api.data.Forms._

    val form: Form[QueryForm] = Form(
      mapping(
        "eventTag"  -> optional(text),
        "actorType" -> optional(text)
      )(QueryForm.apply)(QueryForm.unapply)
    )
  }
}

@Singleton
class ApplicationEventsController @Inject() (
    eventsConnector: EnvironmentAwareApiPlatformEventsConnector,
    applicationEventsView: ApplicationEventsView,
    mcc: MessagesControllerComponents,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService,
    val apmService: ApmService,
    val applicationQueryService: ApplicationQueryService,
    val errorTemplate: ErrorTemplate,
    val errorHandler: ErrorHandler
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with ActionBuilders
    with ApplicationLogger {

  import ApplicationEventsController._

  def page(appId: ApplicationId): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    withApp(appId) { application =>
      def handleFormError(form: Form[QueryForm]): Future[Result] = {
        val queryForm = QueryForm.form.fill(QueryForm.form.bindFromRequest().get)
        for {
          qv     <- eventsConnector.fetchQueryableValues(appId, application.deployedTo)
          events <- eventsConnector.query(appId, application.deployedTo, None, None)
          models  = events.map(EventModel.apply)
        } yield {
          Ok(applicationEventsView(
            QueryModel(
              application.id,
              application.details.name,
              qv,
              models
            ),
            queryForm
          ))
        }
      }

      def handleValidForm(form: QueryForm): Future[Result] = {
        for {
          qv       <- eventsConnector.fetchQueryableValues(appId, application.deployedTo)
          queryForm = QueryForm.form.fill(form)
          events   <- eventsConnector.query(appId, application.deployedTo, form.eventTag, form.actorType)
          models    = events.map(EventModel.apply)
        } yield {
          Ok(applicationEventsView(
            QueryModel(
              application.id,
              application.name,
              qv,
              models
            ),
            queryForm
          ))
        }
      }

      QueryForm.form.bindFromRequest().fold(handleFormError, handleValidForm)
    }
  }
}
