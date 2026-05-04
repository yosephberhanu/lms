FROM eclipse-temurin:21-jre

WORKDIR /app

# Expects the bootJar output to exist at build time (run `gradle :config:bootJar` first).
# Copy only the executable Spring Boot jar (not a "-plain.jar").
COPY build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "/app/app.jar"]