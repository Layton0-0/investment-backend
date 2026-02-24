# Spring Boot backend. Build in repo, image push by CI.
FROM eclipse-temurin:17-jre-alpine
ENV TZ=Asia/Seoul
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=Asia/Seoul", "-jar", "app.jar"]
