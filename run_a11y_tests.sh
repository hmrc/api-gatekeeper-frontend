#!/usr/bin/env bash
sbt -Dbrowser=chrome -Daccessibility.test='true' acceptance:test sandbox:test