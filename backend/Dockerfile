# ============================================================
# DondeAnime backend - Dockerfile multi-stage
# ------------------------------------------------------------
# Stage 1 (builder): compila el jar con Maven + JDK 21.
# Stage 2 (runtime): copia solo el jar a una imagen JRE mínima.
#
# Beneficio multi-stage: la imagen final NO incluye Maven, ni
# los .m2 caches, ni el código fuente. Solo el jar ejecutable.
# Tamaño aprox: 250 MB (vs ~1 GB si dejáramos todo).
#
# Base eclipse-temurin (Adoptium): build oficial de OpenJDK con
# glibc estándar. Evitamos Alpine en runtime para no enredarnos
# con musl en una app Spring (algunas libs nativas dan guerra).
# ============================================================

# --- STAGE 1: BUILDER ---
FROM eclipse-temurin:25-jdk AS builder

# El wrapper de Maven valida distributionSha256Sum contra el .zip; sin unzip
# cambia solo al .tar.gz y el checksum (calculado para el .zip) no casa.
RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copiamos primero los archivos de configuración Maven para
# cachear la capa de dependencias. Si no cambian pom.xml ni .mvn,
# Docker reutiliza la capa con dependencias descargadas en builds
# sucesivos.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descarga todas las dependencias declaradas. Esta capa solo se
# invalida cuando cambia pom.xml.
RUN ./mvnw dependency:go-offline -B

# Ahora sí copiamos el código y compilamos.
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# --- STAGE 2: RUNTIME ---
FROM eclipse-temurin:25-jre

WORKDIR /app

# Usuario no-root por seguridad. Spring Boot puede ejecutarse como
# cualquier UID; usar root en contenedor es mala práctica aunque
# esté aislado.
RUN groupadd --system spring && useradd --system --gid spring spring
USER spring:spring

# Copiamos solo el jar final del stage builder. Renombramos a
# app.jar para que el comando de arranque sea fijo, independiente
# de la versión del pom.
COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

# Spring Boot escucha en 8080 por convención. Documentamos el puerto
# (EXPOSE no abre nada por sí solo, solo es metadata).
EXPOSE 8080

# JVM tuning para contenedor:
# - MaxRAMPercentage=75 → la JVM usa hasta 75% de la RAM del
#   contenedor (default 25% deja demasiada memoria sin usar).
# - +UseG1GC → GC moderno, buen default para apps web.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC"

# Healthcheck simple: la app responde 200 en /api/anime cuando
# está lista. Docker marca el contenedor como healthy si pasa.
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/api/anime || exit 1

# exec form para que Java sea PID 1 y reciba señales (SIGTERM
# para graceful shutdown).
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
