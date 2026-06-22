# ETAP 1: Budowanie z użyciem oficjalnego JDK 21
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
# Kopiujemy wszystkie pliki projektu
COPY . .
# Nadajemy systemowe uprawnienia do uruchomienia skryptu (bardzo częsty problem Windows -> Linux)
RUN chmod +x ./gradlew
# Odpalamy dokładnie to samo narzędzie, które używa IntelliJ
RUN ./gradlew installDist --no-daemon

# ETAP 2: Lekki kontener do samego działania serwera
FROM eclipse-temurin:21-jre
EXPOSE 8080
WORKDIR /app
# Przerzucamy zbudowany program
COPY --from=build /app/build/install/ /app/

# Automatyczne odpalenie
CMD ["sh", "-c", "find . -type f -path '*/bin/*' ! -name '*.bat' -exec {} \\;"]