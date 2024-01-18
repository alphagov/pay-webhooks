FROM maven:3.9.5-eclipse-temurin-17@sha256:68dd5de443133e0d29a2246139d4b387936e1b1c88891927ebc267d4cc2c4460 AS builder

WORKDIR /home/build
COPY . .

RUN ["mvn", "clean", "--no-transfer-progress", "package", "-DskipTests"]

FROM eclipse-temurin:17-jre@sha256:6d2b9ba869869b9966b74ed592c12dc6febb70e4f00d694de1cbb3a456917c44 AS final

ARG DNS_TTL=15

# Default to UTF-8 file.encoding
ENV LANG C.UTF-8

RUN echo networkaddress.cache.ttl=$DNS_TTL >> "$JAVA_HOME/conf/security/java.security"

RUN apt-get update && apt-get upgrade -y && apt-get install -y tini wget

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
