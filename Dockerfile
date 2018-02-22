ARG NEXUS_VERSION=3.8.0

FROM maven:3-jdk-8-alpine AS build
ARG NEXUS_VERSION=3.8.0
ARG NEXUS_BUILD=02

COPY . /nexus-repository-composer/
RUN cd /nexus-repository-composer/; sed -i "s/3.8.0-02/${NEXUS_VERSION}-${NEXUS_BUILD}/g" pom.xml; \
    mvn clean package;

FROM sonatype/nexus3:$NEXUS_VERSION
ARG NEXUS_VERSION=3.8.0
ARG NEXUS_BUILD=02
ARG COMPOSER_VERSION=0.0.1
ARG TARGET_DIR=/opt/sonatype/nexus/system/org/sonatype/nexus/plugins/nexus-repository-composer/${COMPOSER_VERSION}/
USER root
RUN mkdir -p ${TARGET_DIR}; \
    sed -i 's@nexus-repository-npm</feature>@nexus-repository-npm</feature>\n        <feature prerequisite="false" dependency="false">nexus-repository-composer</feature>@g' /opt/sonatype/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-oss-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml; \
    sed -i 's@<feature name="nexus-repository-npm"@<feature name="nexus-repository-composer" description="org.sonatype.nexus.plugins:nexus-repository-composer" version="0.0.1">\n        <details>org.sonatype.nexus.plugins:nexus-repository-composer2</details>\n        <bundle>mvn:org.sonatype.nexus.plugins/nexus-repository-composer/0.0.1</bundle>\n    </feature>\n    <feature name="nexus-repository-npm"@g' /opt/sonatype/nexus/system/com/sonatype/nexus/assemblies/nexus-oss-feature/${NEXUS_VERSION}-${NEXUS_BUILD}/nexus-oss-feature-${NEXUS_VERSION}-${NEXUS_BUILD}-features.xml;
COPY --from=build /nexus-repository-composer/target/nexus-repository-composer-${COMPOSER_VERSION}.jar ${TARGET_DIR}
USER nexus
