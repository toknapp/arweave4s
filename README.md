
## Arweave4s
![arweave4s logo](logo.png)

[![BuildStatus](https://circleci.com/gh/toknapp/arweave4s.svg?style=svg)](https://circleci.com/gh/toknapp/arweave4s)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://raw.githubusercontent.com/toknapp/arweave4s/develop/COPYING)
[![Latest version](https://maven-badges.herokuapp.com/maven-central/co.upvest/arweave4s-core_2.12/badge.png?style=plastic)](https://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22co.upvest%22%20AND%20a%3A%22arweave4s-core_2.12%22)



### Overview

Arweave4s is a lightweight modular http client for the [Arweave blockchain](https://github.com/ArweaveTeam/arweave/)

The core of Arweave4s is based on the [sttp http client](https://github.com/softwaremill/sttp), this allow to use it with
different [http backends of your choice](http://sttp.readthedocs.io/en/latest/), such as akka-http, Moniz, OkHttp, Scalaz Streams, plain Futures, synchronously and many more.

The response handling is designed to be as flexible as the backend. By providing implicit response handlers for the monad of your choice `Try`, `Future`
`EitherT`, or plain old throw exceptions like it's 1999.  

The current version implements the [following endpoints](https://raw.githubusercontent.com/ArweaveTeam/arweave/master/http_iface_docs.md)

* GET network information
* GET full transaction via ID
* GET specific transaction fields via ID
* GET transaction body as HTML via ID
* GET estimated transaction price
* GET block via ID
* GET block via height
* GET current block
* GET wallet balance via address
* GET last transaction via address
* GET nodes peer list
* POST transaction to network

Additionally the client supports **wallet generation** and **transaction signing**

### Quickstart

Add the following dependency

```
"co.upvest" %% "arweave4s-core" % "0.6.0"
```
then create a configuration context and weave-it-up.

imports:

```scala
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.{higherKinds, postfixOps}

import co.upvest.arweave4s.adt.Data
import co.upvest.arweave4s.adt.Transaction
import co.upvest.arweave4s.adt.Wallet._
import co.upvest.arweave4s.api
import co.upvest.arweave4s.api.Config
// Import response handler for Future
import co.upvest.arweave4s.api.future._

import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import com.softwaremill.sttp.asynchttpclient.future.HttpURLConnectionBackend
```

### Examples

Try starting a node!

```scala
val TestHost0 = (sys.env get "TESTNET_HOST" getOrElse "localhost") + ":1984"
implicit val a = api.Config(host = TestHost0, HttpURLConnectionBackend())

api.block.current()
res1: Block = ...
```

Lets put some data on the Arweave blockchain!

1.  Start Docker env:

    1. Create an account.
    2. Download Docker.
    3. Start Docker by opening the application.

    ```console
    $ git submodule sync
    Synchronizing submodule url for 'arweave'

    $ git submodule update --init --remote
    Submodule path 'arweave': checked out 'b0a1369c319598efd56f95bb9ed3ff74fb068ee0'
    ```

2. build image from a dockerfile to start up an Arweave connection

    ```console
    $ docker-compose run it

    $ docker container ps -a # see container status

    $ docker rm [container-name] # kill dead containers
    ```

3. run App and see cool stuff happen.

    ```scala
    object TestTutorial extends App {

      // Create configuration. Set the sttp's async backend for returning Futures
      val TestHost = (sys.env get "TESTNET_HOST" getOrElse "localhost") + ":1984"
      implicit val c = Config(host = TestHost, AsyncHttpClientFutureBackend())

      // Data to persist on the blockchain.
      val testData = Data("Hi Mom!".getBytes("UTF-8"))

      // Let's get a new wallet
      val wallet = Wallet.generate()

      for {
        // using the API method to estimate the price for the transaction in `Winstons`
        price    <- api.price.estimate(testData)
        // Get the last transaction of the sender wallet
        lastTx   <- api.address.lastTx(wallet)
        // Construct and send the transaction.
        ()       <- api.tx.submit(Transaction.Data(
        id       = Transaction.Id.generate(),
        lastTx   = lastTx,
        owner    = wallet,
        data     = testData,
        reward   = price)
          // Here we actually sign our transaction
        .sign(wallet)
        )
      } yield ()

      println(wallet.owner)
      println(testData)
      println(curBlock)

    }
    ```
    
Voilà, we persisted the data on the blockchain!


### Contact

By questions, comments or suggestions feel free to get in touch by creating an PR or an issue.

For more information to Arweave protocol, check the [Arweave whitepaper](https://www.arweave.org/files/arweave-whitepaper.pdf)
and visit them on [Github](https://github.com/ArweaveTeam/arweave).


### Cavecats [sic](https://www.youtube.com/watch?v=a0SuhNn8S60)

Copyright 2018 Ivan Morozov, Gustav Behm, Tokn GmbH (https://upvest.co)

Arrweave4s is provided to you as free software under the MIT license.
The MIT software license is attached in the [COPYING](COPYING) file.
