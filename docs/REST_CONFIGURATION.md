# REST CONFIGURATION

You can create, update and delete composer repositories through the rest API.

Setup NEXUS_HOST to the nexus url, for instance in case of a local test instance :
NEXUS_HOST=http://localhost:8081

## Hosted repository

### Create
```shell
curl -X 'POST' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/hosted' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-hosted",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "allow_once"
  },
  "cleanup": {
    "policyNames": [
      "string"
    ]
  },
  "component": {
    "proprietaryComponents": true
  }
}'
```

### Get information
```shell
curl -X 'GET' \
  $NEXUS_HOST/service/rest/v1/repositories/composer/hosted/composer-hosted \
  -H 'accept: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true'
```

### Update
```shell
curl -X 'PUT' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/hosted/composer-hosted' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-hosted",
  "format": "composer",
  "url": "$NEXUS_HOST/repository/composer-hosted",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "allow_once"
  },
  "cleanup": {
    "policyNames": [
      "string"
    ]
  },
  "component": {
    "proprietaryComponents": false
  },
  "type": "hosted"
}'
```

## Proxy repository

### Create
```shell
curl -X 'POST' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/proxy' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-proxy",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true
  },
  "cleanup": {
    "policyNames": [
      "string"
    ]
  },
  "proxy": {
    "remoteUrl": "https://packagist.org",
    "contentMaxAge": 1440,
    "metadataMaxAge": 1440
  },
  "negativeCache": {
    "enabled": true,
    "timeToLive": 1440
  },
  "httpClient": {
    "blocked": false,
    "autoBlock": true,
    "connection": {
      "retries": 0,
      "userAgentSuffix": "string",
      "timeout": 60,
      "enableCircularRedirects": false,
      "enableCookies": false,
      "useTrustStore": false
    },
    "authentication": {
      "type": "username",
      "username": "string",
      "password": "string",
      "ntlmHost": "string",
      "ntlmDomain": "string"
    }
  }
}'
```

### Get information

```shell
curl -X 'GET' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/proxy/composer-proxy' \
  -H 'accept: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true'
```

### Update

```shell
curl -X 'PUT' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/proxy/composer-proxy' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-proxy",
  "format": "composer",
  "url": "$NEXUS_HOST/repository/composer-proxy",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": false
  },
  "cleanup": {
    "policyNames": [
      "string"
    ]
  },
  "proxy": {
    "remoteUrl": "https://packagist.org",
    "contentMaxAge": 1440,
    "metadataMaxAge": 1440
  },
  "negativeCache": {
    "enabled": true,
    "timeToLive": 1440
  },
  "httpClient": {
    "blocked": false,
    "autoBlock": true,
    "connection": {
      "retries": 0,
      "userAgentSuffix": "string",
      "timeout": 60,
      "enableCircularRedirects": false,
      "enableCookies": false,
      "useTrustStore": false
    }
  },
  "routingRuleName": null,
  "type": "proxy"
}'
```

## Group repository

### Create
```shell
curl -X 'POST' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/group' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-group",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true
  },
  "group": {
    "memberNames": [
      "composer-hosted"
    ]
  }
}'
```

### Get information

```shell
curl -X 'GET' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/proxy/composer-group' \
  -H 'accept: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true'
```

### Update

```shell
curl -X 'PUT' \
  '$NEXUS_HOST/service/rest/v1/repositories/composer/group/composer-group' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -H 'NX-ANTI-CSRF-TOKEN: 0.5903676711737454' \
  -H 'X-Nexus-UI: true' \
  -d '{
  "name": "composer-group",
  "format": "composer",
  "url": "$NEXUS_HOST/repository/composer-group",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true
  },
  "group": {
    "memberNames": [
      "composer-hosted",
      "composer-proxy"
    ]
  },
  "type": "group"
}'
```