#!/usr/bin/env bash
sbt clean compile coverage test it:test acceptance:test sandbox:test -Dwebdriver.chrome.driver=/Users/sivaisikella/chromedriver coverageReport
python dependencyReport.py api-gatekeeper-frontend
