# ==========================================
# ETAPA 1: Construcción con Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de dependencias primero (mejor cache)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Dar permisos de ejecución
RUN chmod +x mvnw

# Descargar dependencias (esto se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B || true

# Copiar código fuente
COPY src ./src

# Construir la aplicación
# -DskipTests: saltar tests para build más rápido
# -Dmaven.test.skip=true: asegurar que los tests no se ejecuten
# --batch-mode: modo no interactivo
# --no-transfer-progress: menos output verbose
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true --batch-mode --no-transfer-progress

# Verificar que el JAR se creó
RUN ls -lah /app/target/

# ==========================================
# ETAPA 2: Imagen de producción con JRE
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Instalar dependencias del sistema
RUN apk add --no-cache \
    curl \
    tzdata \
    ca-certificates && \
    update-ca-certificates && \
    ln -sf /usr/share/zoneinfo/America/Bogota /etc/localtime && \
    echo "America/Bogota" > /etc/timezone

WORKDIR /app

# Crear usuario no-root
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Crear directorios necesarios
RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

# Cambiar a usuario no-root
USER spring:spring

# Variables de entorno para JVM
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 " \
    SPRING_PROFILES_ACTIVE=prod

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Exponer puerto dinámico de Railway
EXPOSE ${PORT:-8080}

# Comando de inicio optimizado
# Importante: Railway usa la variable PORT, no server.port
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS \
    -Djava.security.egd=file:/dev/./urandom \
    -Dserver.port=${PORT:-8080} \
    -jar app.jar"]