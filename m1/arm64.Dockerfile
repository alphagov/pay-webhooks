FROM maven:3-eclipse-temurin-21@sha256:e0900e5953fddc181fdbe449ba8c27f124470fb545fbbb29f39f50d6093c9b18 AS builder

WORKDIR /home/build
COPY . .

RUN ["mvn", "clean", "--no-transfer-progress", "package", "-DskipTests"]

FROM eclipse-temurin:17-jre@sha256:52aa3cfd024bc60bea6385fd8a4da8af8769af026628d56a34f7ff3977c168a6 AS final

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

RUN apt-get update && apt-get install -y tini wget

COPY import_aws_rds_cert_bundles.sh /
RUN /import_aws_rds_cert_bundles.sh
RUN rm /import_aws_rds_cert_bundles.sh

ENV PORT 8080
ENV ADMIN_PORT 8081

EXPOSE 8080
EXPOSE 8081

WORKDIR /app

COPY --from=builder /home/build/docker-startup.sh .
COPY --from=builder /home/build/target/*.yaml .
COPY --from=builder /home/build/target/pay-*-allinone.jar .

ENTRYPOINT ["tini", "-e", "143", "--"]

CMD ["bash", "./docker-startup.sh"]
