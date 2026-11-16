# ---- Build stage: compile and package the Spring Boot jar ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Download dependencies first so they are cached between builds.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---- Runtime stage: small JRE image, non-root user ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 zoopass && mkdir -p /app/logs && chown zoopass /app/logs
COPY --from=build /app/target/zoopass-backend-*.jar app.jar
USER zoopass
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
