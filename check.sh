#!/usr/bin/env bash

# Initialises port_mappings with the port numbers of all running application using the Service Manager status command.
# port_mappings=$(sm2 -s | grep PASS | awk '{ print $12"->"$12 }' | paste -sd "," -)
  
# The image accepts two environment variables:
# PORT_MAPPINGS: List of ports of the services under test.
# TARGET_IP: IP of the host machine. For Mac this is 'host.docker.internal'. For linux this is 'localhost'
# NOTE:
# When running on a Linux OS, add "--net=host" to the docker run command.
  
IMAGE=artefacts.tax.service.gov.uk/chrome-with-rinetd:latest

# Alternatively, port_mappings can be explicitly initialised as below:
port_mappings="6001->6001,6002->6002,9684->9684"

docker pull ${IMAGE} \
  && docker run \
  -d \
  --rm \
  --name "remote-chrome" \
  --add-host=host.docker.internal:host-gateway \
  -p 4444:4444 \
  -p 5900:5900 \
  -e PORT_MAPPINGS="9684->9684,6001->6001,6003->6003" \
  -e TARGET_IP='host.docker.internal' \
  ${IMAGE}

sbt -Dbrowser="remote-chrome" acceptance:test sandbox:test
