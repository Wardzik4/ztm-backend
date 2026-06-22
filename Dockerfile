# ETAP 1: Budowanie naszej aplikacji używając Gradle i Javy 21
FROM gradle:8.5-jdk21 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle installDist --no-daemon

# ETAP 2: Pakowanie gotowej aplikacji do lekkiego kontenera
FROM eclipse-temurin:21-jre
EXPOSE 8080
WORKDIR /app
COPY --from=build /home/gradle/src/build/install/ /app/

# Automatycznie znajdujemy plik startowy (ignorując windowsowe pliki .bat) i go uruchamiamy!
CMD ["sh", "-c", "find . -type f -path '*/bin/*' ! -name '*.bat' -exec {} \\;"]