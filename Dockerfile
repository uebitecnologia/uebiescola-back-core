# Dockerfile com Layered JARs - cada layer cacheada separadamente.
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw package -Dmaven.test.skip=true -q

FROM eclipse-temurin:17-jdk-alpine AS extract
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN java -Djarmode=layertools -jar app.jar extract

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=extract /app/dependencies/ ./
COPY --from=extract /app/spring-boot-loader/ ./
COPY --from=extract /app/snapshot-dependencies/ ./
COPY --from=extract /app/application/ ./
EXPOSE 8080
ENTRYPOINT ["java", "-Xms64m", "-Xmx256m", "-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=256m", "org.springframework.boot.loader.launch.JarLauncher"]
