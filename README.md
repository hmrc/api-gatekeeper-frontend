
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

