FROM eclipse-temurin:17-jdk AS builder
WORKDIR /build
COPY . .
RUN ./gradlew :app:bootJar --no-daemon -q

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
