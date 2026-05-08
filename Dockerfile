FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
RUN apt-get update && apt-get install -y \
    protobuf-compiler \
    && rm -rf /var/lib/apt/lists/*
RUN protoc --version
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
COPY envoy ./envoy
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
COPY --from=build /app/envoy/envoy.yaml /app/envoy.yaml
COPY --from=build /app/envoy/proto.pb /app/proto.pb
EXPOSE 8080 9900
RUN java -version
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
