FROM sbtscala/scala-sbt:eclipse-temurin-17.0.4_1.8.0_2.13.10
WORKDIR /app
COPY ./dev/app/entrypoint.sh ./entrypoint.sh
COPY ./project/build.properties ./project/build.properties
COPY ./project/plugins.sbt ./project/plugins.sbt
COPY ./dev/configs ./dev/configs
COPY ./build.sbt ./build.sbt
COPY ./src ./src
ENTRYPOINT [ "./entrypoint.sh" ]
