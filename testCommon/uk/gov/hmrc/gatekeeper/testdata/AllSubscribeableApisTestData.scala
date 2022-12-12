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

package uk.gov.hmrc.gatekeeper.testdata

trait AllSubscribeableApisTestData {

  val allSubscribeableApis =
    s"""
       |{
       | "marriage-allowance": {
       |     "serviceName": "marriage-allowance",
       |     "name": "Marriage Allowance",
       |     "isTestSupport": false,
       |     "versions": {
       |         "2.0": {
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         }
       |     }
       | },
       | "api-simulator": {
       |   "serviceName": "api-simulator",
       |   "name": "API Simulator",
       |   "isTestSupport": false,
       |   "versions": {
       |       "1.0": {
       |           "status": "STABLE",
       |           "versionSource": "UNKNOWN",
       |           "access": {
       |               "type": "PUBLIC"
       |           }
       |       }
       |   }
       | },
       | "hello": {
       |   "serviceName": "api-example-microservice",
       |   "name": "Hello World",
       |   "isTestSupport": false,
       |   "versions": {
       |       "1.0": {
       |           "status": "STABLE",
       |           "versionSource": "UNKNOWN",
       |           "access": {
       |               "type": "PUBLIC"
       |           }
       |       }
       |   }
       | },
       |     "notifications": {
       |     "serviceName": "api-notification-pull",
       |     "name": "Pull Notifications",
       |     "isTestSupport": false,
       |     "versions": {
       |         "1.0": {
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         }
       |     }
       | },
       | "test/api-platform-test": {
       |     "serviceName": "api-platform-test",
       |     "name": "API Platform Test",
       |     "isTestSupport": false,
       |     "versions": {
       |         "7.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "6.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "5.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "4.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "3.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.3": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.2": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.1": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "1.0": {
       |             "status": "STABLE",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         }
       |     }
       | },
       | "customs/inventory-linking/exports": {
       |     "serviceName": "customs-inventory-linking-exports",
       |     "name": "Customs Inventory Linking Exports",
       |     "isTestSupport": false,
       |     "versions": {
       |         "2.0": {
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "1.0": {
       |             "status": "BETA",
       |             "versionSource": "UNKNOWN",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         }
       |     }
       | }
       |}
       | """.stripMargin
}

