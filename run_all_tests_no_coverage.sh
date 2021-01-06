#!/usr/bin/env bash
sbt clean compile test it:test acceptance:test sandbox:test
python dependencyReport.py api-gatekeeper-frontend
