FROM eclipse-temurin:24-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :gateway:bootJar -x test --no-daemon && \
    find gateway/build/libs -name "*.jar" ! -name "*plain*" -exec cp {} /app/app.jar \;

FROM eclipse-temurin:24-jre-alpine
WORKDIR /app
RUN apk add --no-cache curl
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]