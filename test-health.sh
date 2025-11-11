#!/bin/bash
curl -s http://localhost:8081/actuator/health | jq .
