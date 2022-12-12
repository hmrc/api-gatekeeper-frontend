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

import play.api.mvc._
import uk.gov.hmrc.gatekeeper.config.AppConfig
import scala.concurrent.ExecutionContext
import com.google.inject.{Inject, Singleton}

import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.services.StrideAuthorisationService

import uk.gov.hmrc.apiplatform.modules.gkauth.services.LdapAuthorisationService
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.gatekeeper.utils.ErrorHelper
import uk.gov.hmrc.gatekeeper.controllers.actions.ActionBuilders
import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.gatekeeper.views.html.applications._
import uk.gov.hmrc.gatekeeper.config.ErrorHandler
import uk.gov.hmrc.gatekeeper.services.ApmService
import uk.gov.hmrc.gatekeeper.services.ApplicationService
import uk.gov.hmrc.gatekeeper.views.html.ErrorTemplate
import uk.gov.hmrc.apiplatform.modules.events.connectors.ApiPlatformEventsConnector
import java.time.format.DateTimeFormatter
import play.api.data.Form
import scala.concurrent.Future
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.ApplicationId
import uk.gov.hmrc.gatekeeper.services.SimpleEventDetails

object ApplicationEventsController {
  case class EventModel(eventDateTime: String, eventTag: String, eventDetails: String, actor: String)

  object EventModel {
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")

    def apply(event: AbstractApplicationEvent): EventModel = {
      EventModel(dateTimeFormatter.format(event.eventDateTime), SimpleEventDetails.typeOfChange(event), SimpleEventDetails.details(event), SimpleEventDetails.who(event))
    }
  }

  case class SearchFilterValues(eventTags: List[String])

  object SearchFilterValues {

    def apply(qvs: QueryableValues): SearchFilterValues = {
      val eventTagDescriptions = qvs.eventTags.map(_.description)
      SearchFilterValues(eventTagDescriptions)
    }
  }

  case class QueryModel(applicationId: ApplicationId, applicationName: String, searchFilterValues: SearchFilterValues, events: Seq[EventModel])

  case class QueryForm(eventTag: Option[String])

  object QueryForm {
    import play.api.data.Forms._

    val form: Form[QueryForm] = Form(
      mapping(
        "eventTag" -> optional(text)
      )(QueryForm.apply)(QueryForm.unapply)
    )
  }
}

@Singleton
class ApplicationEventsController @Inject() (
    eventsConnector: ApiPlatformEventsConnector,
    applicationEventsView: ApplicationEventsView,
    mcc: MessagesControllerComponents,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService,
    val apmService: ApmService,
    val applicationService: ApplicationService,
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
        val queryForm = QueryForm.form.fill(QueryForm.form.bindFromRequest.get)
        for {
          searchFilterValues <- eventsConnector.fetchEventQueryValues(appId)
          events             <- eventsConnector.query(appId, None)
          eventModels         = events.map(EventModel(_))
        } yield {
          val svfs = searchFilterValues.getOrElse(QueryableValues(Nil))
          Ok(applicationEventsView(QueryModel(application.application.id, application.application.name, SearchFilterValues(svfs), eventModels), queryForm))
        }
      }

      def handleValidForm(form: QueryForm): Future[Result] = {
        for {
          searchFilterValues <- eventsConnector.fetchEventQueryValues(appId)
          queryForm           = QueryForm.form.fill(form)
          events             <- eventsConnector.query(appId, form.eventTag.flatMap(EventTags.fromDescription))
          eventModels         = events.map(EventModel(_))
        } yield {
          val svfs = searchFilterValues.getOrElse(QueryableValues(Nil))
          Ok(applicationEventsView(QueryModel(application.application.id, application.application.name, SearchFilterValues(svfs), eventModels), queryForm))
        }
      }

      QueryForm.form.bindFromRequest.fold(handleFormError, handleValidForm)
    }
  }
}
