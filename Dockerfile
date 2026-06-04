# 1. 빌드 단계
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew build.gradle settings.gradle ./
COPY gradle/ gradle/
# 권한 문제 방지
RUN chmod +x gradlew
RUN ./gradlew dependencies
COPY src/ src/
RUN ./gradlew build -x test

# 2. 실행 단계
FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV TZ=Asia/Seoul
ARG ENV=dev

ENTRYPOINT ["java", "-jar", "-Dserver.env=${ENV}", "app.jar"]