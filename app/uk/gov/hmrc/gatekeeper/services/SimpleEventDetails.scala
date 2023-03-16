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

package uk.gov.hmrc.gatekeeper.services

import uk.gov.hmrc.apiplatform.modules.applications.domain.models.Collaborators._
import uk.gov.hmrc.apiplatform.modules.applications.domain.models.{
  Collaborator,
  PrivacyPolicyLocation,
  PrivacyPolicyLocations,
  TermsAndConditionsLocation,
  TermsAndConditionsLocations
}
import uk.gov.hmrc.apiplatform.modules.common.domain.models.{Actor, Actors}
import uk.gov.hmrc.apiplatform.modules.events.applications.domain.models._

object SimpleEventDetails {

  def details(collaborator: Collaborator): String = collaborator match {
    case Administrator(_, email) => s"${email.text} with the role Admin"
    case Developer(_, email)     => s"${email.text} with the role Developer"
  }

  def details(ppl: PrivacyPolicyLocation): String = ppl match {
    case PrivacyPolicyLocations.InDesktopSoftware => "in desktop software"
    case PrivacyPolicyLocations.Url(value)        => value
    case PrivacyPolicyLocations.NoneProvided      => "none provided"
  }

  def details(ppl: TermsAndConditionsLocation): String = ppl match {
    case TermsAndConditionsLocations.InDesktopSoftware => "in desktop software"
    case TermsAndConditionsLocations.Url(value)        => value
    case TermsAndConditionsLocations.NoneProvided      => "none provided"
  }

  // format: off
  def typeOfChange(evt: ApplicationEvent): String = evt match {
    case e: ApiSubscribedEvent                                => "Subscribed to API"
    case e: ApiSubscribedV2                                   => "Subscribed to API"
    case e: ApiUnsubscribedEvent                              => "Unsubscribed from API"
    case e: ApiUnsubscribedV2                                 => "Unsubscribed from API"
    case e: CollaboratorAddedV2                               => "Team member added"
    case e: CollaboratorRemovedV2                             => "Team member removed"
    case e: TeamMemberAddedEvent                              => "Team member added"
    case e: TeamMemberRemovedEvent                            => "Team member removed"
    case e: ClientSecretAddedV2                               => "Client secret added"
    case e: ClientSecretRemovedV2                             => "Client secret removed"
    case e: ClientSecretAddedEvent                            => "Client secret added"
    case e: ClientSecretRemovedEvent                          => "Client secret removed"
    case e: PpnsCallBackUriUpdatedEvent                       => "PPNS Callback URL changed"
    case e: RedirectUrisUpdatedV2                             => "Redirect URIs changed"
    case e: RedirectUrisUpdatedEvent                          => "Redirect URIs changed"
    case e: ResponsibleIndividualChanged                      => "Responsible individual changed"
    case e: ResponsibleIndividualChangedToSelf                => "Responsible individual changed to self"
    case e: ResponsibleIndividualDeclined                     => "Responsible individual declined"
    case e: ResponsibleIndividualDeclinedUpdate               => "Responsible individual declined update"
    case e: ResponsibleIndividualDidNotVerify                 => "Responsible individual did not verify"
    case e: ResponsibleIndividualDeclinedOrDidNotVerify       => "Responsible individual declined or did not verify"
    case e: ResponsibleIndividualSet                          => "Responsible individual set"
    case e: ResponsibleIndividualVerificationStarted          => "Responsible individual verification started"
    case e: ApplicationStateChanged                           => "State changed"
    case e: ApplicationApprovalRequestDeclined                => "Approval declined"
    case e: TermsOfUsePassed                                  => "Terms of use passed"
    case e: ProductionAppNameChangedEvent                     => "Application name changed"
    case e: ProductionAppPrivacyPolicyLocationChanged         => "Privacy policy URL changed"
    case e: ProductionAppTermsConditionsLocationChanged       => "Terms and conditions URL changed"
    case e: ProductionLegacyAppPrivacyPolicyLocationChanged   => "Privacy policy URL changed"
    case e: ProductionLegacyAppTermsConditionsLocationChanged => "Terms and conditions URL changed"
    case e: ApplicationDeleted                                => "Application deleted"
    case e: ApplicationDeletedByGatekeeper                    => "Application deleted by Gatekeeper user"
    case e: ProductionCredentialsApplicationDeleted           => "Production credentials request deleted"
  }

 def details(evt: ApplicationEvent): String = evt match {
    case e: ApiSubscribedEvent                                => s"${e.context} ${e.version}"
    case e: ApiSubscribedV2                                   => s"${e.context.value} ${e.version.value}"
    case e: ApiUnsubscribedEvent                              => s"${e.context} ${e.version}"
    case e: ApiUnsubscribedV2                                 => s"${e.context.value} ${e.version.value}"
    case e: CollaboratorAddedV2                               => details(e.collaborator)
    case e: CollaboratorRemovedV2                             => details(e.collaborator)
    case e: TeamMemberAddedEvent                              => s"${e.teamMemberEmail.text} with the role ${e.teamMemberRole}"
    case e: TeamMemberRemovedEvent                            => s"${e.teamMemberEmail.text} with the role ${e.teamMemberRole}"
    case e: ClientSecretAddedV2                               => s"Client secret ${e.clientSecretName}"
    case e: ClientSecretRemovedV2                             => s"Client secret ${e.clientSecretName}"
    case e: ClientSecretAddedEvent                            => s"Client secret ${e.clientSecretId}"
    case e: ClientSecretRemovedEvent                          => s"Client secret ${e.clientSecretId}"
    case e: PpnsCallBackUriUpdatedEvent                       => s"${e.boxName}: ${e.newCallbackUrl}"
    case e: RedirectUrisUpdatedV2                             => s"Redirect URIs: ${e.newRedirectUris.mkString}"
    case e: RedirectUrisUpdatedEvent                          => s"Redirect URIs: ${e.newRedirectUris.mkString}"
    case e: ResponsibleIndividualChanged                      => s"Responsible individual: ${e.newResponsibleIndividualName} (${e.newResponsibleIndividualEmail.text})"
    case e: ResponsibleIndividualChangedToSelf                => s"Responsible individual: ${e.requestingAdminName} (${e.requestingAdminEmail.text})"
    case e: ResponsibleIndividualDeclined                     => s"Responsible individual declined ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ResponsibleIndividualDeclinedUpdate               => s"Responsible individual declined update ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ResponsibleIndividualDidNotVerify                 => s"Responsible individual did not verify ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ResponsibleIndividualDeclinedOrDidNotVerify       => s"Responsible individual declined or did not verify ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ResponsibleIndividualSet                          => s"Responsible individual set to ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ResponsibleIndividualVerificationStarted          => s"Responsible individual verification started by ${e.responsibleIndividualName} (${e.responsibleIndividualEmail.text})"
    case e: ApplicationStateChanged                           => s"State changes ${e.newAppState}"
    case e: ApplicationApprovalRequestDeclined                => s"Approval declined by ${e.decliningUserName} (${e.decliningUserEmail.text})"
    case e: TermsOfUsePassed                                  => s"Terms of use passed"
    case e: ProductionAppNameChangedEvent                     => s"Application name: ${e.newAppName}"
    case e: ProductionAppPrivacyPolicyLocationChanged         => s"Privacy policy URL: ${details(e.newLocation)}"
    case e: ProductionAppTermsConditionsLocationChanged       => s"Terms and conditions URL: ${details(e.newLocation)}"
    case e: ProductionLegacyAppPrivacyPolicyLocationChanged   => s"Privacy policy URL: ${e.newUrl}"
    case e: ProductionLegacyAppTermsConditionsLocationChanged => s"Terms and conditions URL: ${e.newUrl}"
    case e: ApplicationDeleted                                => s"Application deleted - ${e.reasons}"
    case e: ApplicationDeletedByGatekeeper                    => s"Application deleted - ${e.reasons}"
    case e: ProductionCredentialsApplicationDeleted           => s"Production credentials request deleted - ${e.reasons}"
  }
  // format: on

  def who(event: ApplicationEvent): String = applicationEventWho(event.actor)

  def applicationEventWho(actor: Actor): String = actor match {
    case Actors.AppCollaborator(email) => email.text
    case Actors.GatekeeperUser(user)   => s"(GK) $user"
    case Actors.ScheduledJob(jobId)    => s"Job($jobId)"
    case Actors.Unknown                => "Unknown"
  }

}
