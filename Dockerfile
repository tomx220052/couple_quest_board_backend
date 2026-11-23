# syntax=docker/dockerfile:1

# ---------- 建置 ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# 先只複製建置設定，依賴沒變時可重用快取層
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp dependency:go-offline

COPY src src
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -ntp -DskipTests package

# ---------- 執行 ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV TZ=Asia/Taipei

RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/target/app.jar app.jar
USER app

EXPOSE 3000
ENTRYPOINT ["java", "-Duser.timezone=Asia/Taipei", "-jar", "/app/app.jar"]
