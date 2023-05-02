FROM eclipse-temurin:17-jre@sha256:00749e73c4ba59065c0a9fe1e97ab93f78f97dcb1ad4545981b2a7b8527bb83c

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

RUN apt-get update && apt-get install -y \
  tini \
  wget

# Add RDS CA certificates to the default truststore
RUN wget -qO - https://s3.amazonaws.com/rds-downloads/rds-ca-2019-root.pem       | keytool -import -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias rds-ca-2019-root \
 && wget -qO - https://s3.amazonaws.com/rds-downloads/rds-combined-ca-bundle.pem | keytool -import -trustcacerts -keystore -cacerts -storepass changeit -noprompt -alias rds-combined-ca-bundle

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

COPY docker-startup.sh /app/docker-startup.sh
COPY target/*.yaml /app/
COPY target/pay-*-allinone.jar /app/

ENTRYPOINT ["tini", "-e", "143", "--"]

CMD ["bash", "./docker-startup.sh"]
