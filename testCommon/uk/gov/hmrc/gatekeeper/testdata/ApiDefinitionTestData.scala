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

  val listOfApiDefinitions =
    s"""
       |[
       | {
       |   "serviceName": "employersPayeAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Employers PAYE",
       |   "description": "EMPLOYERS PAYE API.",
       |   "context": "employers-paye",
       |   "versions": [
       |     {
       |       "version": "1.0",
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
       |           "scope": "read:employers-paye-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   ],
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-07-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       |  {
       |   "serviceName": "payeCreditsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Paye Credits",
       |   "description": "PAYE CREDITS API",
       |   "context": "paye-credits",
       |   "versions": [
       |     {
       |       "version": "1.0",
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
       |           "scope": "read:paye-credits-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   ],
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2022-07-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       |  {
       |   "serviceName": "individualBenefitsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Individual Benefits",
       |   "description": "INDIVIDUAL BENEFITS API.",
       |   "context": "individual-benefits",
       |   "versions": [
       |     {
       |       "version": "1.0",
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
       |           "scope": "read:individual-benefits-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   ],
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-06-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       |   {
       |   "serviceName": "selfAssessmentAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Self Assessment",
       |   "description": "SELF ASSESSMENT API.",
       |   "context": "self-assessment",
       |   "versions": [
       |     {
       |       "version": "1.0",
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
       |           "scope": "read:self-assessment-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   ],
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-04-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | }
       |]
  """.stripMargin

  val mapOfApiDefinitions =
    s"""
       |{
       | "employers-paye" : {
       |   "serviceName": "employersPayeAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Employers PAYE",
       |   "description": "EMPLOYERS PAYE API.",
       |   "context": "employers-paye",
       |   "versions": {
       |     "1.0": {
       |       "version": "1.0",
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
       |           "scope": "read:employers-paye-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-07-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       | "paye-credits": {
       |   "serviceName": "payeCreditsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Paye Credits",
       |   "description": "PAYE CREDITS API",
       |   "context": "paye-credits",
       |   "versions": {
       |     "1.0": {
       |       "version": "1.0",
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
       |           "scope": "read:paye-credits-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2022-07-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       | "individual-benefits": {
       |   "serviceName": "individualBenefitsAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Individual Benefits",
       |   "description": "INDIVIDUAL BENEFITS API.",
       |   "context": "individual-benefits",
       |   "versions": {
       |     "1.0": {
       |       "version": "1.0",
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
       |           "scope": "read:individual-benefits-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-06-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | },
       | "self-assessment": {
       |   "serviceName": "selfAssessmentAPI",
       |   "serviceBaseUrl": "http://localhost/",
       |   "name": "Self Assessment",
       |   "description": "SELF ASSESSMENT API.",
       |   "context": "self-assessment",
       |   "versions": {
       |     "1.0": {
       |       "version": "1.0",
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
       |           "scope": "read:self-assessment-1",
       |           "queryParameters": []
       |         }
       |       ],
       |       "endpointsEnabled": true,
       |       "versionSource": "UNKNOWN"
       |     }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2023-04-06T09:13:40.439Z",
       |   "categories": [ "OTHER"]
       | }
       |}
  """.stripMargin

}
