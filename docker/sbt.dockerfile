FROM upvest/scala-sbt-docker-k8:stable

RUN mkdir /arweave4s
WORKDIR /arweave4s

ADD project project
ADD build.sbt .
RUN sbt update

ADD core core
RUN sbt it:compile

ENTRYPOINT ["sbt"]
