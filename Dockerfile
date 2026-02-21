#
# Multi-stage build for Spring Boot (Java 21)
#

FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copy Maven wrapper + pom first (better layer cache)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

RUN ./mvnw -B -ntp -DskipTests dependency:go-offline

COPY src/ src/

RUN ./mvnw -B -ntp -DskipTests package \
  && JAR_FILE="$(ls -1 target/*.jar | grep -v 'original-' | head -n 1)" \
  && test -n "$JAR_FILE" \
  && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ENV JAVA_OPTS=""

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
