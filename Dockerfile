FROM maven:3.9.5-eclipse-temurin-17-alpine@sha256:5f0dc07092c3806ff7cf7c3f21b0e93d30befc5d56adf03758d301c05d63efb8 AS builder

WORKDIR /home/build
COPY . .

RUN ["mvn", "clean", "--no-transfer-progress", "package", "-DskipTests"]

FROM eclipse-temurin:17-jre-alpine@sha256:aa5fc621cc5eebbf90b815973e95ca800cfcab671fdd7918a3a5c9c770bb804a AS final

RUN ["apk", "--no-cache", "upgrade"]

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

COPY ./import_aws_rds_cert_bundles.sh /
RUN /import_aws_rds_cert_bundles.sh && rm /import_aws_rds_cert_bundles.sh

RUN ["apk", "add", "--no-cache", "bash", "tini"]

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
