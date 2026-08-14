FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -pl nz-app -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S nz && adduser -S -G nz nz
WORKDIR /app
RUN mkdir -p /app/uploads && chown -R nz:nz /app
COPY --from=build --chown=nz:nz /workspace/nz-app/target/nz-app-*.jar /app/app.jar

USER nz
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
  CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
