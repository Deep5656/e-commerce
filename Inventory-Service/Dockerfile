FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/Inventory-Service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
