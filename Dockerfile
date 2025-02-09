# declaration of NEXUS_VERSION must appear before first FROM command
# see: https://docs.docker.com/reference/dockerfile/#understand-how-arg-and-from-interact
ARG NEXUS_VERSION=latest

FROM maven:3-eclipse-temurin-17 AS build
COPY . /nexus-repository-composer/
WORKDIR /nexus-repository-composer
RUN ./mvnw clean package -PbuildKar

FROM sonatype/nexus3:$NEXUS_VERSION

ARG DEPLOY_DIR=/opt/sonatype/nexus/deploy/
USER root
COPY --from=build /nexus-repository-composer/nexus-repository-composer/target/nexus-repository-composer-*-bundle.kar ${DEPLOY_DIR}

# Remove official Composer plugin from features (part of the Community Edition since 3.77.0)
RUN if [ -f "$NEXUS_HOME"/system/com/sonatype/nexus/assemblies/nexus-community-feature/*/nexus-community-feature-*-features.xml ]; then \
    sed -i 's#<feature[^>]*>nexus-repository-composer</feature>##' "$NEXUS_HOME"/system/com/sonatype/nexus/assemblies/nexus-community-feature/*/nexus-community-feature-*-features.xml ; \
    fi

USER nexus
