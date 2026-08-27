FROM eclipse-temurin:21-jdk AS build

WORKDIR /source
COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY push-protocol ./push-protocol
COPY push-server ./push-server
RUN --mount=type=cache,target=/root/.gradle \
    mkdir app baselineprofile && chmod +x gradlew && ./gradlew :push-server:installDist --no-daemon

FROM eclipse-temurin:21-jre

RUN useradd --system --uid 10001 --create-home dankchat && install -d -o dankchat -g dankchat /data
WORKDIR /app
COPY --from=build /source/push-server/build/install/push-server/ /app/
USER dankchat

ENV HOST=0.0.0.0
ENV PORT=8080
ENV DATA_DIRECTORY=/data
EXPOSE 8080
VOLUME ["/data"]

ENTRYPOINT ["/app/bin/push-server"]
