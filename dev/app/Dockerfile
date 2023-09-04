ARG SCALA_VERSION

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.9.4_2.13.11 AS scala-213

FROM sbtscala/scala-sbt:eclipse-temurin-focal-17.0.5_8_1.9.4_3.3.0 AS scala-3

FROM scala-${SCALA_VERSION}
WORKDIR /app
COPY ./dev/app/entrypoint.sh ./entrypoint.sh
COPY ./project/build.properties ./project/build.properties
COPY ./project/plugins.sbt ./project/plugins.sbt
COPY ./dev/configs ./dev/configs
COPY ./build.sbt ./build.sbt
COPY ./src ./src
ENTRYPOINT [ "./entrypoint.sh" ]
