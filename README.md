# Cracker

## Setup:

```bash
sudo extism lib install latest
mvn -N wrapper:wrapper

```

Cracker launcher is a Sprites launcher

> This is a WIP 🚧

## Install Extism

## Release (how to)
 - Change version number in `.env`
 - Change version number in `pom.xml`
 - Change version number in `MainVerticle.java`
 - build
 - Change version number in `docker/.env`
 - Change version number in `docker/Dockerfile` (`ENV CRACKER_VERSION="0.0.N"`)