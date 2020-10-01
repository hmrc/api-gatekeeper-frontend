package acceptance.testdata

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
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "6.0": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "5.0": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "4.0": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "3.0": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.3": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.2": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.1": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "2.0": {
       |             "status": "STABLE",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "1.0": {
       |             "status": "STABLE",
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
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         },
       |         "1.0": {
       |             "status": "BETA",
       |             "access": {
       |                 "type": "PUBLIC"
       |             }
       |         }
       |     }
       | }
       |}
       | """.stripMargin
}

