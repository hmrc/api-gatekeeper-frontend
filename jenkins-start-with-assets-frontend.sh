#!/bin/bash

echo "Starting ASSETS"

cd $WORKSPACE
rm -rf service-manager-config
git clone git@github.tools.tax.service.gov.uk:HMRC/service-manager-config.git

sm --stop ALL
sm --cleanlogs
sm --start ASSETS_FRONTEND -r --wait 60 --noprogress

echo "Running tests for Api Documentation Frontend"

cd $WORKSPACE

echo "Start tests..."
sbt clean test acceptance:test sandbox:test

echo "Publish..."
sbt dist-tgz publish

echo "Gracefully shutdown server..."

sm --stop ALL
