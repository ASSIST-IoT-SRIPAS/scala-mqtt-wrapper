FROM debian:stable-20221114-slim AS base
ARG SCALA_VERSION=2.13.10
ARG SBT_VERSION=1.8.0
ARG JAVA_VERSION=17.0.4-tem
SHELL [ "/bin/bash", "-o", "pipefail", "-c" ]
ENV SDKMAN_DIR=/root/.sdkman
RUN apt-get update && \
    apt-get install -y \
    curl=7.74* \
    unzip=6.0* \
    zip=3.0* \
    && \
    apt-get autoremove -y && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    curl -s "https://get.sdkman.io" | bash && \
    echo "sdkman_auto_answer=true" > "$SDKMAN_DIR/etc/config" && \
    echo "sdkman_auto_selfupdate=false" >> "$SDKMAN_DIR/etc/config" && \
    source "/root/.sdkman/bin/sdkman-init.sh" && \
    sdk install scala "$SCALA_VERSION" && \
    sdk install sbt "$SBT_VERSION" && \
    sdk install java "$JAVA_VERSION" && \
    mkdir sbt-warmup && \
    (cd sbt-warmup && sbt --version) && \
    rm -r sbt-warmup
ENV SCALA_HOME="$SDKMAN_DIR/candidates/scala/current"
ENV SBT_HOME="$SDKMAN_DIR/candidates/sbt/current"
ENV JAVA_HOME="$SDKMAN_DIR/candidates/java/current"
ENV PATH="$SCALA_HOME/bin:$SBT_HOME/bin:$JAVA_HOME/bin:$PATH"
WORKDIR /app
COPY ./dev/app/entrypoint.sh ./entrypoint.sh
COPY ./project/build.properties ./project/build.properties
COPY ./project/plugins.sbt ./project/plugins.sbt
COPY ./dev/configs ./dev/configs
COPY ./build.sbt ./build.sbt
COPY ./src ./src
ENTRYPOINT [ "./entrypoint.sh" ]
