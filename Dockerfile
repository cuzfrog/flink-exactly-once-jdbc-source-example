FROM openjdk:17-alpine
RUN addgroup -S app && adduser -S app -G app
USER app:app

WORKDIR /app
COPY dist/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
