# Contributing guide
TODO

## Ways to contribute
TODO

## How to contribute
TODO

## Development environment
For development, run `app3.sh` (Scala 3) or `app213.sh` (Scala 2.13), and `env.sh` scripts.

### Application
Copy a file from examples to a directory with pekko or akka module, and name it `Main.scala` (i.e., `modules/pekko/src/main/scala/Main.scala`).
The `app3.sh` and `app213.sh` scripts will start the application in the development mode.
```shell
# first terminal

./dev/scripts/pekko/app3.sh
or
./dev/scripts/pekko/app213.sh
or
./dev/scripts/akka/app213.sh
```

### Environment
The `env.sh` script will start the development environment.
```shell
# second terminal

./dev/scripts/env.sh
```

## Project structure
TODO

## Code style
TODO

## Releases
Commits to main will trigger a SNAPSHOT release.
To create a full release, create a tag on the main branch.
```shell
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```
The releases should be pushed to Maven Central.
