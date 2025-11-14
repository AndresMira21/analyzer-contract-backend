# ==========================================
# ETAPA 1: Construcción con Maven
# ==========================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar archivos de Maven para cachear dependencias
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Dar permisos de ejecución al Maven Wrapper
RUN chmod +x mvnw

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Compilar la aplicación (sin tests para build más rápido)
RUN ./mvnw clean package -DskipTests

# ==========================================
# ETAPA 2: Imagen de producción con JRE
# ==========================================
FROM eclipse-temurin:21-jre-alpine

# Instalar curl para health checks y configurar zona horaria
RUN apk add --no-cache curl tzdata && \
    ln -sf /usr/share/zoneinfo/America/Bogota /etc/localtime && \
    echo "America/Bogota" > /etc/timezone

WORKDIR /app

# Crear usuario no-root para seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# Copiar el JAR compilado desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

# Crear directorios necesarios
RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

# Cambiar al usuario no-root
USER spring:spring

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod

# Health check automático
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:${PORT:-8080}/actuator/health || exit 1

# Railway proporciona la variable PORT automáticamente
EXPOSE ${PORT:-8080}

# Comando para iniciar la aplicación
# Railway inyecta PORT automáticamente, por eso usamos $PORT
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]