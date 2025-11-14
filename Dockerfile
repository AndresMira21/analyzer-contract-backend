# ==========================================
# ETAPA 1: Construcción con Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B

COPY src ./src
RUN ./mvnw clean package -DskipTests

# ==========================================
# ETAPA 2: Imagen de producción con JRE
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Instalar curl, ca-certificates y actualizar certificados SSL
RUN apk add --no-cache curl tzdata ca-certificates && \
    update-ca-certificates && \
    ln -sf /usr/share/zoneinfo/America/Bogota /etc/localtime && \
    echo "America/Bogota" > /etc/timezone

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

USER spring:spring

ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

EXPOSE ${PORT:-8080}

# Agregado: Deshabilitar validación de hostname SSL para MongoDB
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Djavax.net.ssl.trustStore=/opt/java/openjdk/lib/security/cacerts -Dserver.port=${PORT:-8080} -jar app.jar"]