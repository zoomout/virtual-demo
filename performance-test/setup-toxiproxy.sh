#!/bin/bash

# Configuration
TOXIPROXY_URL="http://localhost:8474"

echo "Configuring Toxiproxy at ${TOXIPROXY_URL}..."

# Create Postgres Proxy
# Host 'postgres' is used because toxiproxy container is in the same docker network
curl -s -X POST "${TOXIPROXY_URL}/proxies" \
     -H "Content-Type: application/json" \
     -d '{
           "name": "postgres",
           "listen": "0.0.0.0:54320",
           "upstream": "postgres:5432",
           "enabled": true
         }'
echo "Postgres proxy created on port 54320"

# Create Wiremock Proxy
# Host 'wiremock' is used because toxiproxy container is in the same docker network
curl -s -X POST "${TOXIPROXY_URL}/proxies" \
     -H "Content-Type: application/json" \
     -d '{
           "name": "wiremock",
           "listen": "0.0.0.0:8081",
           "upstream": "wiremock:8080",
           "enabled": true
         }'
echo "Wiremock proxy created on port 8081"

# Example of adding latency to Postgres
# curl -s -X POST "${TOXIPROXY_URL}/proxies/postgres/toxics" \
#      -H "Content-Type: application/json" \
#      -d '{
#            "name": "latency",
#            "type": "latency",
#            "attributes": {
#              "latency": 500,
#              "jitter": 100
#            }
#          }'
