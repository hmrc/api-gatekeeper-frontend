#!/usr/bin/env bash
sbt clean compile coverage test sandbox:test acceptance:test coverageReport