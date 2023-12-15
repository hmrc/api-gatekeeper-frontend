#!/bin/bash

sm2 --start DATASTREAM AUTH AUTH_LOGIN_API AUTH_LOGIN_STUB TIME_BASED_ONE_TIME_PASSWORD STRIDE_AUTH_FRONTEND STRIDE_AUTH STRIDE_IDP_STUB API_PUBLISHER USER_DETAILS EMAIL


sm2 --start THIRD_PARTY_APPLICATION INTERNAL_AUTH THIRD_PARTY_DEVELOPER API_DEFINITION API_SCOPE API_SUBSCRIPTION_FIELDS API_PLATFORM_EVENTS API_PLATFORM_MICROSERVICE THIRD_PARTY_DELEGATED_AUTHORITY API_GATEKEEPER_APPROVALS_FRONTEND API_PLATFORM_XML_SERVICES API_GATEKEEPER_XML_SERVICES_FRONTEND PUSH_PULL_NOTIFICATIONS_API THIRD_PARTY_ORCHESTRATOR

./run_local.sh
