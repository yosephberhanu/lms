FROM eclipse-temurin:21-jre

WORKDIR /app

# Expects the bootJar output to exist at build time (run `gradle :property:bootJar` first).
COPY build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
