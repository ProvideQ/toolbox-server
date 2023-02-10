# ProvideQ Toolbox Server
This repository contains the source code for the [ProvideQ](https://provideq.org) Toolbox server.
A web-based user interface can be found in our
[ProvideQ/ProvideQ repository](https://github.com/ProvideQ/ProvideQ).

## Development setup
1. Install Java 17 (check with `java -version`)
2. Clone this repository
3. Run the server using `./gradlew bootRun`

## Deployment
This repository is designed to be deployed with [Dokku](https://dokku.com/) but you can also run 
the Java application directly or inside a docker container (`Dockerfile` is included!).
The docker container can be built and run as follows:
```shell
docker build --tag provideq-toolbox-backend --build-arg GAMS_LICENSE=$(base64 -w 0 ./gamslice.txt) .
docker run --publish 8080:5000 provideq-toolbox-backend
```

## License
Copyright (c) 2022 - 2023 ProvideQ

This project is available under the [MIT License](./LICENSE).