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

package utils

import model.{APIDefinition, DetailedSubscribedApplicationResponse, SubscribedApplicationResponse, SubscriptionDetails}
import play.api.Logger

trait SubscriptionEnhancer {

  def combine(appResponses: Seq[SubscribedApplicationResponse],
              definitions: Seq[APIDefinition]): Seq[DetailedSubscribedApplicationResponse] = {
    appResponses.map { ar =>
      val details = ar.subscriptionNames.map(str => SubscriptionDetails(definitions.find(_.context == str) match {
        case Some(x) => x.name
        case _ => Logger.warn(s"Could not map subscription $str to an existing context"); str
      }, str))
      DetailedSubscribedApplicationResponse(ar.id, ar.name, ar.description, ar.collaborators, ar.createdOn, ar.state, details)
    }
  }
}

object SubscriptionEnhancer extends SubscriptionEnhancer