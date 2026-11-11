# Stage 1: Build file JAR bằng JDK 21
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src/ src/
RUN ./mvnw clean package -DskipTests

# Stage 2: Chạy runtime bằng JRE 21 (Jammy hỗ trợ đầy đủ ImageIO/AWT cho OcrServiceImpl)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080

ENTRYPOINT ["java", "-Xms128m", "-Xmx280m", "-XX:MaxMetaspaceSize=128m", "-XX:+UseSerialGC", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]