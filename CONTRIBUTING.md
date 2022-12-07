# Contributing guide
TODO

## Ways to contribute
TODO

## How to contribute
TODO

## Development environment
For development, run `app.sh` and `env.sh` scripts:
```
# first terminal

./dev/scripts/app.sh
```

```
# second terminal

./dev/scripts/env.sh
```

The `app.sh` script will start the application in the development mode.
The `env.sh` script will start the development environment.

## Project structure
TODO

## Code style
TODO

## Releases
Commits to main will trigger a SNAPSHOT release.

To create a full release, create a tag on the main branch like so:

```shell
git tag -a v0.1.0 -m "v0.1.0"
git push origin v0.1.0
```

The releases should be pushed to Maven Central.
