
# Usa Maven con Java 21 para compilar
FROM maven:3.9-eclipse-temurin-21 AS build

# Establece directorio de trabajo
WORKDIR /app

# Copia archivos de Maven (para cachear dependencias)
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Descarga dependencias (se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copia el código fuente
COPY src ./src

# Compila la aplicación (sin tests para build más rápido)
RUN ./mvnw clean package -DskipTests


# Usa solo JRE (más ligero que JDK)
FROM eclipse-temurin:21-jre-alpine

# Instala curl para health checks
RUN apk add --no-cache curl tzdata && \
    ln -sf /usr/share/zoneinfo/America/Bogota /etc/localtime

WORKDIR /app

# Crea usuario no-root (seguridad)
RUN addgroup -S spring && adduser -S spring -G spring

# Copia el JAR compilado desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar

# Crea directorios necesarios
RUN mkdir -p /app/uploads /app/logs && \
    chown -R spring:spring /app

# Cambia al usuario no-root
USER spring:spring

# Variables de entorno por defecto
ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=prod

# Health check automático
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Puerto de la aplicación
EXPOSE 8080

# Comando para iniciar la app
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]