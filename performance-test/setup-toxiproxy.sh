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

# Add Postgres latency
 curl -s -X POST "${TOXIPROXY_URL}/proxies/postgres/toxics" \
      -H "Content-Type: application/json" \
      -d '{
            "name": "latency",
            "type": "latency",
            "attributes": {
              "latency": 2,
              "jitter": 1
            }
          }'

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

# Add wiremock latency
curl -s -X POST "${TOXIPROXY_URL}/proxies/wiremock/toxics" \
     -H "Content-Type: application/json" \
     -d '{
           "name": "wiremock_latency",
           "type": "latency",
           "stream": "downstream",
           "attributes": {
             "latency": 500,
             "jitter": 50
           }
         }'

echo "Added 500ms latency to Wiremock proxy"
