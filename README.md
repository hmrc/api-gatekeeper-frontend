
# API Gatekeeper Frontend

[![Build Status](https://travis-ci.org/hmrc/api-gatekeeper-frontend.svg?branch=master)](https://travis-ci.org/hmrc/api-gatekeeper-frontend) [ ![Download](https://api.bintray.com/packages/hmrc/releases/api-gatekeeper-frontend/images/download.svg) ](https://bintray.com/hmrc/releases/api-gatekeeper-frontend/_latestVersion)

This service provides a frontend HMRC's internal users to vet requests to create applications 
as part of our HMRC third party tax software and [application programming interface (API) strategy](http://developer.service.hmrc.gov.uk/api-documentation).

## Summary

This service provides the following functionalites:

* Ability to log in / log out with an internal user
* Role based access control for actions

## Requirements 

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE] to run.

## Run the application

To run the application execute

```
sbt 'run 9000' 
```

and then access the application at

```
http://localhost:9000/api-gatekeeper/login
```

## Authentication

The application is starting up with a login screen, it is using an internal authentication and authorisation service.
At the moment it is not yet possible to stub the them. 

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

