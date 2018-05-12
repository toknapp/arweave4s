FROM erlang:20-alpine

EXPOSE 1984

RUN apk update && apk add make git
ENV REPOSITORY https://github.com/ArweaveTeam/arweave
ENV REFSPEC master

RUN mkdir /arweave
WORKDIR /arweave

RUN git init
RUN git remote add origin $REPOSITORY
RUN git fetch origin $REFSPEC
RUN git checkout FETCH_HEAD

ADD 0001-dev-chain-settings.patch .
RUN git apply *.patch

RUN make all

ENTRYPOINT ["./arweave-server"]
