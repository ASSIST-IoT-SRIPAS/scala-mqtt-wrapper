ARG SCALA_VERSION

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.8.1_1_1.9.6_2.13.12 AS scala-213

FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.8.1_1_1.9.6_3.3.1 AS scala-3

FROM scala-${SCALA_VERSION}
ARG LIBRARY_VERSION
WORKDIR /app
COPY ./dev/app/entrypoint_"${LIBRARY_VERSION}".sh ./entrypoint.sh
COPY ./project/build.properties ./project/build.properties
COPY ./project/plugins.sbt ./project/plugins.sbt
COPY ./dev/configs ./dev/configs
COPY ./build.sbt ./build.sbt
COPY ./modules ./modules
ENTRYPOINT [ "./entrypoint.sh" ]
