FROM eclipse-temurin:25-jre
WORKDIR /app
COPY build/libs/server-0.1.0.jar app.jar
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
