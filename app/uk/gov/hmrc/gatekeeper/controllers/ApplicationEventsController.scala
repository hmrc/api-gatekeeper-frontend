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

package uk.gov.hmrc.gatekeeper.controllers

import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.gatekeeper.config.AppConfig
import scala.concurrent.ExecutionContext
import com.google.inject.{Singleton, Inject}

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationService
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.services.ApplicationService
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.apiplatform.modules.events.connectors.ApiPlatformEventsConnector
import java.time.format.DateTimeFormatter
import uk.gov.hmrc.apiplatform.modules.events.domain.models._
import play.api.data.Form
import scala.concurrent.Future
import play.api.mvc.{Range => _, _}
 
object ApplicationEventsController {
  case class EventModel(eventDateTime: String, eventType: String, eventDetails: String, actor: String)
  
  object EventModel {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")
    def apply(event: ApplicationEvent): EventModel = {
      EventModel(dateTimeFormatter.format(event.eventDateTime), EventType.describe(ApplicationEvent.asEventTypeValue(event)), "Something", ApplicationEvent.extractActorText(event))
    }
  }

  case class SearchFilterValues(years: Seq[String], eventTypes: Seq[String], actors: Seq[String])

  object SearchFilterValues {
    def apply(qvs: QueryableValues): SearchFilterValues = {
      val years = Range(qvs.firstYear, qvs.lastYear+1).map(_.toString)
      val eventTypes = qvs.eventTypes.map(EventType.describe)
      val actors = qvs.actors
      SearchFilterValues(years, eventTypes, actors)
    }
  }

  case class QueryModel(applicationId: ApplicationId, applicationName: String, searchFilterValues: SearchFilterValues, events: Seq[EventModel])

  case class QueryForm(year: Option[String], eventType: Option[String], actor: Option[String])

  object QueryForm {
    import play.api.data.Forms._

    val form: Form[QueryForm] = Form(
      mapping(
        "year" -> optional(text),
        "eventType" -> optional(text),
        "actor" -> optional(text)
      )(QueryForm.apply)(QueryForm.unapply)
    )
  }
}

@Singleton
class ApplicationEventsController @Inject()(
  eventsConnector: ApiPlatformEventsConnector,
  applicationEventsView: ApplicationEventsView,
  mcc: MessagesControllerComponents,
  strideAuthorisationService: StrideAuthorisationService,
  val ldapAuthorisationService: LdapAuthorisationService,
  val apmService: ApmService,
  val applicationService: ApplicationService,
  val errorTemplate: ErrorTemplate,
  val errorHandler: ErrorHandler
)(implicit val appConfig: AppConfig, override val ec: ExecutionContext)
    extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper 
    with ActionBuilders 
    with ApplicationLogger {

  import ApplicationEventsController._

  def page(appId: ApplicationId): Action[AnyContent] = anyAuthenticatedUserAction { implicit request =>
    withApp(appId) { application =>
      def handleFormError(form: Form[QueryForm]): Future[Result]  = {
        for {
          searchFilterValues <- eventsConnector.fetchEventQueryValues(appId)
          queryForm = QueryForm.form.fill(QueryForm.form.bindFromRequest.get)
          events <- eventsConnector.query(appId, None,None,None)
          eventModels = events.map(EventModel(_))
        } yield 
          searchFilterValues.fold(NotFound(""))(sfvs => BadRequest(applicationEventsView(QueryModel(application.application.id, application.application.name, SearchFilterValues(sfvs), eventModels), queryForm)))
      }

      def handleValidForm(form: QueryForm): Future[Result] = {
        for {
          searchFilterValues <- eventsConnector.fetchEventQueryValues(appId)
          queryForm = QueryForm.form.fill(form)
          events <- eventsConnector.query(appId, form.year.map(_.toInt),form.eventType.flatMap(EventType.fromDescription),form.actor)
          eventModels = events.map(EventModel(_))
        } yield 
          searchFilterValues.fold(NotFound(""))(sfvs => BadRequest(applicationEventsView(QueryModel(application.application.id, application.application.name, SearchFilterValues(sfvs), eventModels), queryForm)))
      }
      
      QueryForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }
}
