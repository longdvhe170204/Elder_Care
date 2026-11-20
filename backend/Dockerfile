FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app
# Thử lại khi Maven Central trả 429/503 (giới hạn tải từ máy CI)
ENV MAVEN_ARGS="-Daether.connector.http.retryHandler.count=6 -Daether.connector.http.retryHandler.interval=10000"
COPY pom.xml .
# Tải cả plugin để bước package ít phải ra mạng; lớp này được cache khi pom.xml không đổi
RUN mvn -B -q dependency:go-offline dependency:resolve-plugins
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:25-jre
ENV TZ=Asia/Ho_Chi_Minh
WORKDIR /app
COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]