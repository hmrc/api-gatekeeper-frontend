#!/usr/bin/env bash
sbt -mem 4000 clean compile coverage test acceptance:test sandbox:test coverageReport
python dependencyReport.py api-gatekeeper-frontend
