# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Cache dependencies first
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline
# Build the app
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---- Run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Render free chỉ 512MB RAM -> siết heap + metaspace + code cache để không OOM
ENV JAVA_OPTS="-XX:MaxRAMPercentage=50 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=48m -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
# Render/Railway/Koyeb tự inject biến PORT -> bind vào đó, fallback 8080 khi chạy local
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
