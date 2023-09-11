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

package uk.gov.hmrc.gatekeeper.testdata

trait ApiDefinitionTestData {

  val apiDefinition =
    s"""
       |[
       | {
       |   "serviceName": "employersPayeAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Employers PAYE",
       |   "description": "EMPLOYERS PAYE API.",
       |    "deployedTo": "PRODUCTION",
       |   "context": "employers-paye",
       |   "versions": [
       |     {
       |       "version": "1.0",
       |       "versionSource": "UNKNOWN",
       |       "status": "STABLE",
       |       "access": {
       |         "type": "PUBLIC"
       |       },
       |       "endpoints": [
       |         {
       |           "uriPattern": "/qwerty",
       |           "endpointName": "employersPayeAPI",
       |           "method": "GET",
       |           "authType": "USER",
       |           "throttlingTier": "UNLIMITED",
       |           "scope": "read:employers-paye-1"
       |         }
       |       ]
       |     }
       |   ],
       |   "requiresTrust": false
       | },
       |  {
       |   "serviceName": "payeCreditsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Paye Credits",
       |   "description": "PAYE CREDITS API",
       |    "deployedTo": "PRODUCTION",
       |   "context": "paye-credits",
       |   "versions": [
       |     {
       |       "version": "1.0",
       |       "versionSource": "UNKNOWN",
       |       "status": "DEPRECATED",
       |       "access": {
       |         "type": "PUBLIC"
       |       },
       |       "endpoints": [
       |         {
       |           "uriPattern": "/qwerty",
       |           "endpointName": "payeCreditsAPI",
       |           "method": "GET",
       |           "authType": "USER",
       |           "throttlingTier": "UNLIMITED",
       |           "scope": "read:paye-credits-1"
       |         }
       |       ]
       |     }
       |   ],
       |   "requiresTrust": false
       | },
       |  {
       |   "serviceName": "individualBenefitsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Individual Benefits",
       |   "description": "INDIVIDUAL BENEFITS API.",
       |    "deployedTo": "PRODUCTION",
       |   "context": "individual-benefits",
       |   "versions": [
       |     {
       |       "version": "1.0",
       |       "versionSource": "UNKNOWN",
       |       "status": "STABLE",
       |       "access": {
       |         "type": "PUBLIC"
       |       },
       |       "endpoints": [
       |         {
       |           "uriPattern": "/qwerty",
       |           "endpointName": "individualBenefitsAPI",
       |           "method": "GET",
       |           "authType": "USER",
       |           "throttlingTier": "UNLIMITED",
       |           "scope": "read:individual-benefits-1"
       |         }
       |       ]
       |     }
       |   ],
       |   "requiresTrust": false
       | },
       |   {
       |   "serviceName": "selfAssessmentAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Self Assessment",
       |   "description": "SELF ASSESSMENT API.",
       |    "deployedTo": "PRODUCTION",
       |   "context": "self-assessment",
       |   "versions": [
       |     {
       |       "version": "1.0",
       |       "versionSource": "UNKNOWN",
       |       "status": "STABLE",
       |       "access": {
       |         "type": "PUBLIC"
       |       },
       |       "endpoints": [
       |         {
       |           "uriPattern": "/qwerty",
       |           "endpointName": "selfAssessmentAPI",
       |           "method": "GET",
       |           "authType": "USER",
       |           "throttlingTier": "UNLIMITED",
       |           "scope": "read:self-assessment-1"
       |         }
       |       ]
       |     }
       |   ],
       |   "requiresTrust": false
       | }
       |]
  """.stripMargin

}
