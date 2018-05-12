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
RUN git checkout $REFSPEC
RUN echo "building git ref: $(git show-ref -s --head HEAD)"

ADD 0001-docker-friendly-dev-chain.patch .
RUN git apply 0001-docker-friendly-dev-chain.patch

RUN make all

ENTRYPOINT ["./arweave-server"]
