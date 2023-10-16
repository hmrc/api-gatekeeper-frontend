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

trait AllSubscribeableApisTestData {

  val allSubscribeableApis =
    s"""
       |{
       | "marriage-allowance": {
       |     "serviceName": "marriage-allowance",
       |     "serviceBaseUrl": "https://marriage-allowance.protected.mdtp",
       |     "name": "Marriage Allowance",
       |     "description": "Marriage Allowance",
       |     "context": "marriage-allowance",
       |     "isTestSupport": false,
       |     "versions": {
       |         "2.0": {
       |             "version": "2.0",
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                {
       |                    "uriPattern": "/sa/{utr}/status",
       |                    "endpointName": "Get marriage allowance status",
       |                    "method": "GET",
       |                    "authType": "USER",
       |                    "throttlingTier": "UNLIMITED",
       |                    "scope": "read:marriage-allowance",
       |                    "queryParameters": []
       |                }
       |            ],
       |            "endpointsEnabled": true,
       |            "versionSource": "UNKNOWN"
       |         }
       |     },
       |     "requiresTrust": false,
       |     "isTestSupport": false,
       |     "lastPublishedAt": "2018-07-06T08:10:17.852Z",
       |     "categories": [
       |         "SELF_ASSESSMENT"
       |     ]
       | },
       | "api-simulator": {
       |   "serviceName": "api-simulator",
       |   "serviceBaseUrl": "https://api-simulator.protected.mdtp",
       |   "name": "API Simulator",
       |   "description": "API Simulator",
       |   "context": "api-simulator",
       |   "versions": {
       |       "1.0": {
       |           "version": "1.0",
       |           "status": "STABLE",
       |           "versionSource": "UNKNOWN",
       |           "access": {
       |               "type": "PUBLIC"
       |           },
       |           "endpoints": [
       |               {
       |                   "uriPattern": "/world",
       |                   "endpointName": "Open API with Get",
       |                   "method": "GET",
       |                   "authType": "USER",
       |                   "throttlingTier": "UNLIMITED",
       |                   "scope": "read:api-simulator",
       |                   "queryParameters": []
       |               }
       |            ],
       |            "endpointsEnabled": true,
       |            "versionSource": "UNKNOWN"
       |       }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2018-08-06T08:10:17.852Z",
       |   "categories": [
       |       "OTHER"
       |   ]
       | },
       | "hello": {
       |   "serviceName": "api-example-microservice",
       |   "serviceBaseUrl": "https://hello.protected.mdtp",
       |   "name": "Hello World",
       |   "description": "API Example Microservice",
       |   "context": "hello",
       |   "versions": {
       |       "1.0": {
       |           "version": "1.0",
       |           "status": "STABLE",
       |           "versionSource": "UNKNOWN",
       |           "access": {
       |               "type": "PUBLIC"
       |           },
       |           "endpoints": [
       |               {
       |                   "uriPattern": "/world",
       |                   "endpointName": "Open API with Get",
       |                   "method": "GET",
       |                   "authType": "USER",
       |                   "throttlingTier": "UNLIMITED",
       |                   "scope": "read:hello",
       |                   "queryParameters": []
       |                }
       |            ],
       |            "endpointsEnabled": true,
       |            "versionSource": "UNKNOWN"
       |       }
       |   },
       |   "requiresTrust": false,
       |   "isTestSupport": false,
       |   "lastPublishedAt": "2018-09-06T08:10:17.852Z",
       |   "categories": [
       |       "OTHER"
       |   ]
       | },
       | "notifications": {
       |     "serviceName": "api-notification-pull",
       |     "serviceBaseUrl": "https://api-notification-pull.protected.mdtp",
       |     "name": "Pull Notifications",
       |     "description": "Allows for retrieval of notifications",
       |     "context": "notifications",
       |     "versions": {
       |         "1.0": {
       |             "version": "1.0",
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/conversationId/{conversationId}",
       |                     "endpointName": "Get all notifications for a conversation",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:notifications",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         }
       |     },
       |     "requiresTrust": false,
       |     "isTestSupport": false,
       |     "lastPublishedAt": "2018-10-06T08:10:17.852Z",
       |     "categories": [
       |         "CUSTOMS"
       |     ]
       | },
       | "test/api-platform-test": {
       |     "serviceName": "api-platform-test",
       |     "serviceBaseUrl": "https://api-platform-test.protected.mdtp",
       |     "name": "API Platform Test",
       |     "description": "API Platform Test",
       |     "context": "test/api-platform-test",
       |     "versions": {
       |         "7.0": {
       |             "version": "7.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "6.0": {
       |             "version": "6.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "5.0": {
       |             "version": "5.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "4.0": {
       |             "version": "4.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "3.0": {
       |             "version": "3.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "2.3": {
       |             "version": "2.3",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "2.2": {
       |             "version": "2.2",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "2.1": {
       |             "version": "2.1",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "2.0": {
       |             "version": "2.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         },
       |         "1.0": {
       |             "version": "1.0",
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/hello/world",
       |                     "endpointName": "Get hello world",
       |                     "method": "GET",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "read:api-platform-test",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "UNKNOWN"
       |         }
       |     },
       |     "requiresTrust": false,
       |     "isTestSupport": false,
       |     "lastPublishedAt": "2018-11-06T08:10:17.852Z",
       |     "categories": [
       |         "OTHER"
       |     ]
       | },
       | "customs/inventory-linking/exports": {
       |     "serviceName": "customs-inventory-linking-exports",
       |     "serviceBaseUrl": "https://customs-inventory-linking-exports.protected.mdtp",
       |     "name": "Customs Inventory Linking Exports",
       |     "description": "Customs Inventory Linking Exports",
       |     "context": "customs/inventory-linking/exports",
       |     "versions": {
       |         "2.0": {
       |             "version": "2.0",
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/",
       |                     "endpointName": "Inventory Exports Request",
       |                     "method": "POST",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "write:customs-inventory-linking-exports",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "OAS"
       |         },
       |         "1.0": {
       |             "version": "1.0",
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             },
       |             "endpoints": [
       |                 {
       |                     "uriPattern": "/",
       |                     "endpointName": "Inventory Exports Request",
       |                     "method": "POST",
       |                     "authType": "USER",
       |                     "throttlingTier": "UNLIMITED",
       |                     "scope": "write:customs-inventory-linking-exports",
       |                     "queryParameters": []
       |                 }
       |              ],
       |              "endpointsEnabled": true,
       |              "versionSource": "OAS"
       |         }
       |     },
       |     "requiresTrust": false,
       |     "isTestSupport": false,
       |     "lastPublishedAt": "2018-12-06T08:10:17.852Z",
       |     "categories": [
       |         "CUSTOMS"
       |     ]
       | }
       |}
       | """.stripMargin
}
