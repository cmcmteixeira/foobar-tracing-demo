FROM java:openjdk-8-jre-alpine
ENV SBT_VERSION "1.1.2"
ENV SCALA_VERSION 2.12.4

RUN apk add --no-cache curl tar bash
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

RUN curl -L "https://github.com/sbt/sbt/releases/download/v$SBT_VERSION/sbt-$SBT_VERSION.tgz" | tar -xz -C /root && \
    ln -s /root/sbt/bin/sbt /usr/local/bin/sbt && \
    chmod 0755 /usr/local/bin/sbt && \
    sbt sbtVersion
RUN mkdir /app
RUN mkdir -p /root/.ivy2
WORKDIR /app
COPY . /app

ARG service
ENV SERVICE_ENV=$service
ARG version
ENV VERSION_ENV=$version

RUN sbt "project $SERVICE_ENV" universal:packageBin
RUN unzip /app/$SERVICE_ENV/target/universal/$SERVICE_ENV*.zip
RUN mkdir /build
RUN ls /app/$SERVICE_ENV/target/universal/
RUN mv ${SERVICE_ENV}-${VERSION_ENV}-SNAPSHOT /build/app
RUN mv /build/app/bin/${SERVICE_ENV} /build/app/bin/service

FROM java:openjdk-8-jre-alpine
RUN apk add --no-cache bash
RUN mkdir /app
COPY --from=0 /build/app /app
CMD "/app/bin/service"

