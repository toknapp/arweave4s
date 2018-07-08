# Contributing

1.  Fork the repository.

2.  Set upstream:

        $ git remote add upstream git@github.com:toknapp/arweave4s.git

3.  Branch from develop:

        $ git checkout develop
        $ git checkout -b feature-x

4.  Make one or more atomic commits, and ensure that each commit has a
    descriptive commit message.

5. Update existing Spec or create one.    

6.  Run tests, and address any errors.

  $ sbt test
  $ sbt integration-test

7.  Push:

        $ git push origin feature-x

8.  Open a pull request against develop.
