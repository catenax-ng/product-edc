FROM gradle:jdk11
# https://docs.docker.com/develop/develop-images/multistage-build/#use-multi-stage-builds

RUN apt-get update && apt-get install -y maven

WORKDIR /build

# JAVA build options - 512m is not enough
ENV _JAVA_OPTIONS="-Xmx2048m"

COPY . .

# to cache gradle distribution download
ENV GRADLE_USER_HOME=/build/.gradle
RUN cd ./edc && ./gradlew --version

# build EDC first
RUN cd ./edc && ./gradlew ${MAVEN_OPTS} clean build publishToMavenLocal -x test

RUN ./mvnw -pl .,edc-extensions/business-partner-validation -am package

#  -Pwith-docker-image - of course not possible in the build container
RUN ./mvnw -pl .,edc-controlplane/edc-controlplane-memory -am package

RUN find ./target
# create default cert
# TODO: can we use localhost.localdomain? Do we need an "email" as described in the Readme?
RUN mkdir /certs
RUN openssl req -newkey rsa:2048 -new -nodes -x509 -days 3650 -subj "/CN=localhost.localdomain" -keyout /certs/key.pem -out /certs/cert.pem
# SECURITY: default password used!
RUN openssl pkcs12 -inkey /certs/key.pem -in /certs/cert.pem -export -out /certs/cert.pfx -password pass:123456


FROM openjdk:11-jre-slim-buster
ARG JAR
ARG HTTP_PROXY

ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:8090
ENV SPRING_PROFILES_ACTIVE local,basic
ENV IDSADAPTER_SERVICEURL http://registry-service:4243
ENV IDSADAPTER_CONNECTORTYPE edc
ENV IDSADAPTER_CONNECTORPROTOCOL http
ENV IDSADAPTER_CONNECTORHOST provider-control-plane:9191
ENV IDSADAPTER_CONNECTORURL http://provider-control-plane:9191
ENV IDSADAPTER_CONNECTORUSER X-Api-Key
ENV IDSADAPTER_CONNECTORPASSWORD 123456
ENV AASPROXY_REGISTRYURL http://registry-service:4243
ENV IDSADAPTER_ADAPTERURL http://provider-aas-service:4244
ENV AASPROXY_WRAPPERURL http://api-wrapper:9191/api/service

RUN if [[ -n "${HTTP_PROXY}"  ]]; then echo "Acquire::http::Proxy \"${HTTP_PROXY}\"" >> /etc/apt/apt.conf.d/proxy.conf; fi && \
    if [[ -n "${HTTP_PROXY}" ]]; then echo "Acquire::https::Proxy \"${HTTP_PROXY}\"" >> /etc/apt/apt.conf.d/proxy.conf; fi && \
    apt-get -y upgrade && \
    apt-get -y update && \
    apt-get -y install curl && \
    if [[ -n "${HTTP_PROXY}"  ]]; then rm -f /etc/apt/apt.conf.d/proxy.conf; fi

WORKDIR /app

# copy the relevant jar from the build image
COPY --from=0 /jars/$JAR app.jar
RUN ls -la

# copy default cert built in the build image
RUN mkdir /certs
COPY --from=0 /certs/* /certs/

EXPOSE 9191
EXPOSE 8090

ENTRYPOINT java \
    -Didsadapter.serviceUrl=${IDSADAPTER_SERVICEURL}\
    -Didsadapter.connectorType=${IDSADAPTER_CONNECTORTYPE}\
    -Didsadapter.connectorProtocol=${IDSADAPTER_CONNECTORPROTOCOL}\
    -Didsadapter.connectorHost=${IDSADAPTER_CONNECTORHOST}\
    -Didsadapter.connectorUrl=${IDSADAPTER_CONNECTORURL}\
    -Didsadapter.connectorUser=${IDSADAPTER_CONNECTORUSER}\
    -Didsadapter.connectorPassword=${IDSADAPTER_CONNECTORPASSWORD}\
    -Didsadapter.adapterUrl=${IDSADAPTER_ADAPTERURL}\
    -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE}\
    -Daasproxy.registryUrl=${AASPROXY_REGISTRYURL}\
    -Daasproxy.wrapperUrl=${AASPROXY_WRAPPERURL}\
    -Djava.security.edg=file:/dev/.urandom \
    -jar app.jar
