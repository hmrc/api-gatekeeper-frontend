
# API Gatekeeper Frontend

[![Build Status](https://travis-ci.org/hmrc/api-gatekeeper-frontend.svg?branch=master)](https://travis-ci.org/hmrc/api-gatekeeper-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gatekeeper-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/api-gatekeeper-frontend/_latestVersion)

This service provides a frontend HMRC's internal users to vet requests to create applications 
as part of our HMRC third party tax software and [application programming interface (API) strategy](http://developer.service.hmrc.gov.uk/api-documentation).

## Summary

This service provides the following functionality:

* Ability to log in / log out with an internal user
* Role based access control for actions

## Requirements 

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To run the application use the `run_local_with_dependencies.sh` script to start the service along with all of
the back end dependencies that it needs (which are started using Service Manager). You will need to have added
a suitable user in the Auth database in your local MongoDB. 

Once everything is up and running you can access the application at

```
http://localhost:9684/api-gatekeeper/login
```

Alternatively, the `run_in_stub_mode.sh` script will run the service in "stub" mode against mocked back end services.

## Unit tests
```
sbt test
```

## Acceptance tests
```
sbt acceptance:test
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").

## ZAP testing
Once the api-gatekeeper-frontend is running locally, it can be tested using OWASP Zed Attack Proxy (ZAP) which is a security tool that can be used to highlight any potential vulnerabilities: - 
* Download and install [ZAP](https://www.owasp.org/index.php/OWASP_Zed_Attack_Proxy_Project).
* Install the latest scanners for ZAP by `Managing Add-ons` and adding `Active scanner rules (beta)` 
* Ensure your local machine has Web Proxy (HTTP) enabled and local host set to the Port ZAP is running on (e.g.`11000`).
* ZAP can be started to run on a specific port by running `/Applications/OWASP\ ZAP.app/Contents/Java/zap.sh -port 11000` in terminal.

Various security tests can be run within ZAP and the different types of attacks are dependent on the service under test. In order to setup different tests and reporting thresholds: -
* Navigate to the `Scan Policy Manager` within the `Analyse` menu option.
* Within the `Scan Policy Manager` create a new policy and set the different reporting and attack thresholds.
* Providing the proxy settings above are set, ZAP can monitor the local requests when certain user actions are completed.
* Once the request appears in ZAP, right click on it and select `Attack` and `Active Scan`
* Select the policy tab and set the appropriate policy for the service under test.
* Select to start the scan.
* Once the scan is complete the security tests run against the service are displayed in the ZAP interface.
* Reports can also be generated and saved in various formats from the ZAP Report menu option.
* When running Zap tests on api-gatekeeper-frontend, the following user actions are an example of what can be included in the tests:
  * Login 
  * Filter App 
  * Check App 
  * Approve App
  * Decline App 
  * Filter Dev 
  * View Dev 
  * Logout


