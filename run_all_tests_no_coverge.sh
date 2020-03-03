#!/usr/bin/env bash
sbt -mem 4000 clean compile test it:test acceptance:test sandbox:test
python dependencyReport.py api-gatekeeper-frontend
