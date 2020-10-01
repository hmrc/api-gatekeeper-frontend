package acceptance.testdata

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
       |       "status": "PUBLISHED",
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
       |       "status": "PUBLISHED",
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
       |       "status": "PUBLISHED",
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

