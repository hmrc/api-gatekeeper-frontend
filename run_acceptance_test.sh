#!/bin/bash

sbt clean test
sbt acceptance:test
sbt sandbox:test

