# Build multi-stage do sing-core.
# Java/Maven sao arch-neutral: builda igual em x86_64 e arm64.

# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Camada de dependencias separada: so refaz o download se o pom mudar.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd -r -u 1001 spring && chown spring /app
COPY --from=build /src/target/sing-core-*.jar /app/app.jar
USER spring

EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
