FROM eclipse-temurin:21-jre

WORKDIR /app

COPY build/libs/*-SNAPSHOT.jar /app/app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
