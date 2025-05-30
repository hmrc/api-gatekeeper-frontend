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

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future.successful
import scala.concurrent.{ExecutionContext, Future}

import play.api.data.Form
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}

import uk.gov.hmrc.apiplatform.modules.common.services.ApplicationLogger
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.GatekeeperBaseController
import uk.gov.hmrc.apiplatform.modules.gkauth.controllers.actions.GatekeeperAuthorisationActions
import uk.gov.hmrc.apiplatform.modules.gkauth.domain.models.LoggedInRequest
import uk.gov.hmrc.apiplatform.modules.gkauth.services.{LdapAuthorisationService, StrideAuthorisationService}
import uk.gov.hmrc.apiplatform.modules.tpd.emailpreferences.domain.models.EmailTopic
import uk.gov.hmrc.apiplatform.modules.tpd.emailpreferences.domain.models.EmailTopic.{BUSINESS_AND_POLICY, EVENT_INVITES, RELEASE_SCHEDULES, TECHNICAL}
import uk.gov.hmrc.apiplatform.modules.tpd.mfa.domain.models.MfaType
import uk.gov.hmrc.gatekeeper.config.AppConfig
import uk.gov.hmrc.gatekeeper.models.Forms.RemoveEmailPreferencesForm
import uk.gov.hmrc.gatekeeper.models._
import uk.gov.hmrc.gatekeeper.models.xml.XmlOrganisation
import uk.gov.hmrc.gatekeeper.services.{ApiDefinitionService, DeveloperService, XmlService}
import uk.gov.hmrc.gatekeeper.utils.CsvHelper.ColumnDefinition
import uk.gov.hmrc.gatekeeper.utils.{CsvHelper, ErrorHelper, UserFunctionsWrapper}
import uk.gov.hmrc.gatekeeper.views.html.developers.{DevelopersView, _}
import uk.gov.hmrc.gatekeeper.views.html.{ErrorTemplate, ForbiddenView}

@Singleton
class DevelopersController @Inject() (
    val forbiddenView: ForbiddenView,
    developerService: DeveloperService,
    val apiDefinitionService: ApiDefinitionService,
    xmlService: XmlService,
    mcc: MessagesControllerComponents,
    developersView: DevelopersView,
    removeEmailPreferencesView: RemoveEmailPreferences,
    override val errorTemplate: ErrorTemplate,
    strideAuthorisationService: StrideAuthorisationService,
    val ldapAuthorisationService: LdapAuthorisationService
  )(implicit val appConfig: AppConfig,
    override val ec: ExecutionContext
  ) extends GatekeeperBaseController(strideAuthorisationService, mcc)
    with GatekeeperAuthorisationActions
    with ErrorHelper
    with UserFunctionsWrapper
    with ApplicationLogger {

  def blankDevelopersPage() = anyAuthenticatedUserAction { implicit request =>
    combineUsersIntoPage(Future.successful(List.empty), DevelopersSearchForm(None, None, None, None))
  }

  def developersCsv() = atLeastSuperUserAction { implicit request =>
    {

      def isMfaTypeActive(user: RegisteredUser, mfaType: MfaType): Boolean                    = {
        user.mfaDetails.exists(mfa => (mfa.verified && mfa.mfaType == mfaType))
      }
      def getNumberOfXmlOrganisations(user: RegisteredUser, orgs: List[XmlOrganisation]): Int = {
        orgs.filter(org => org.collaborators.exists(coll => coll.userId == user.userId)).size
      }
      def topicSubscribedTo(user: RegisteredUser, topic: EmailTopic): Boolean                 = {
        user.emailPreferences.topics.contains(topic)
      }
      def categoriesSubscribedTo(user: RegisteredUser): Int                                   = {
        user.emailPreferences.interests.count(r => r.services.isEmpty)
      }
      def individualApisSubscribedTo(user: RegisteredUser): Int                               = {
        user.emailPreferences.interests.map(r => r.services.size).sum
      }
      def getLastLogin(user: RegisteredUser): String                                          = {
        user.lastLogin.map(_.toString).getOrElse("")
      }
      def getRegistrationTime(user: RegisteredUser): String                                   = {
        user.registrationTime.map(_.toString).getOrElse("")
      }

      def csvColumnDefinitions(orgs: List[XmlOrganisation]) = Seq[ColumnDefinition[RegisteredUser]](
        ColumnDefinition("UserId", (dev => dev.userId.toString())),
        ColumnDefinition("SMS MFA Active", (dev => isMfaTypeActive(dev, MfaType.SMS).toString())),
        ColumnDefinition("Authenticator MFA Active", (dev => isMfaTypeActive(dev, MfaType.AUTHENTICATOR_APP).toString())),
        ColumnDefinition("Business And Policy Email", (dev => topicSubscribedTo(dev, BUSINESS_AND_POLICY).toString())),
        ColumnDefinition("Technical Email", (dev => topicSubscribedTo(dev, TECHNICAL).toString())),
        ColumnDefinition("Release Schedules Email", (dev => topicSubscribedTo(dev, RELEASE_SCHEDULES).toString())),
        ColumnDefinition("Event Invites Email", (dev => topicSubscribedTo(dev, EVENT_INVITES).toString())),
        ColumnDefinition("Full Category Emails", (dev => categoriesSubscribedTo(dev).toString())),
        ColumnDefinition("Individual APIs Emails", (dev => individualApisSubscribedTo(dev).toString())),
        ColumnDefinition("XML Vendors", (dev => getNumberOfXmlOrganisations(dev, orgs).toString())),
        ColumnDefinition("Failed login attempts", (dev => dev.failedLogins.toString())),
        ColumnDefinition("Last Login", (dev => getLastLogin(dev))),
        ColumnDefinition("Developer Hub Registration Date", (dev => getRegistrationTime(dev)))
      )

      (
        for {
          allUsers <- developerService.fetchUsers()
          users     = allUsers.filter(_.verified)
          orgs     <- xmlService.getAllOrganisations()
        } yield (users, orgs)
      )
        .map(usersAndOrgs => CsvHelper.toCsvString(csvColumnDefinitions(usersAndOrgs._2), usersAndOrgs._1))
        .map(Ok(_).withHeaders(CONTENT_DISPOSITION -> s"attachment; filename=developers-${Instant.now()}.csv").as("text/csv"))
    }

  }

  def developersPage() = anyAuthenticatedUserAction { implicit request =>
    DevelopersSearchForm.form.bindFromRequest().fold(
      formWithErrors => {
        logger.warn("Errors found trying to bind request for developers search")
        // binding failure, you retrieve the form containing errors:
        Future.successful(BadRequest(developersView(Seq.empty, "", Seq.empty, DevelopersSearchForm.form.fill(DevelopersSearchForm(None, None, None, None)))))
      },
      searchParams => {
        val allFoundUsers = searchParams match {
          case DevelopersSearchForm(None, None, None, None)                                                                      =>
            logger.info("Not performing a query for empty parameters")
            Future.successful(List.empty)
          case DevelopersSearchForm(maybeEmailFilter, maybeApiVersionFilter, maybeEnvironmentFilter, maybeDeveloperStatusFilter) =>
            logger.info("Searching developers")
            val filters =
              DevelopersSearchFilter(
                mapEmptyStringToNone(maybeEmailFilter),
                ApiContextVersion(mapEmptyStringToNone(maybeApiVersionFilter)),
                ApiSubscriptionInEnvironmentFilter(maybeEnvironmentFilter),
                DeveloperStatusFilter(maybeDeveloperStatusFilter)
              )
            developerService.searchDevelopers(filters)
        }
        combineUsersIntoPage(allFoundUsers, searchParams)
          .map(_.withHeaders("Cache-Control" -> "no-cache"))
      }
    )
  }

  def combineUsersIntoPage(allFoundUsers: Future[List[AbstractUser]], searchParams: DevelopersSearchForm)(implicit request: LoggedInRequest[_]) = {
    for {
      users          <- allFoundUsers
      registeredUsers = users.collect {
                          case r: RegisteredUser => r
                        }
      verifiedUsers   = registeredUsers.filter(_.verified)
      apiVersions    <- apiDefinitionService.fetchAllApiDefinitions()
      form            = DevelopersSearchForm.form.fill(searchParams)
    } yield Ok(developersView(users, usersToEmailCopyText(verifiedUsers), getApiVersionsDropDownValues(apiVersions), form))
  }

  def removeEmailPreferencesPage(): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    successful(Ok(removeEmailPreferencesView(RemoveEmailPreferencesForm.form, false)))
  }

  def removeEmailPreferencesAction(): Action[AnyContent] = atLeastSuperUserAction { implicit request =>
    def handleValidForm(form: RemoveEmailPreferencesForm): Future[Result]         = {
      developerService.removeEmailPreferencesByService(form.serviceName) map {
        case EmailPreferencesDeleteSuccessResult => Ok(removeEmailPreferencesView(RemoveEmailPreferencesForm.form.fill(form), true))
        case EmailPreferencesDeleteFailureResult => technicalDifficulties
      }
    }
    def handleInvalidForm(form: Form[RemoveEmailPreferencesForm]): Future[Result] = {
      successful(BadRequest(removeEmailPreferencesView(form, false)))
    }
    RemoveEmailPreferencesForm.form.bindFromRequest().fold(handleInvalidForm, handleValidForm)
  }
}
