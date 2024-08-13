# Step 1: Build the toolbox-server Java application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Install dependencies first
COPY gradle gradle
COPY gradlew gradlew
RUN chmod +x ./gradlew
COPY build.gradle build.gradle

RUN ./gradlew dependencies

# Then, build the application
COPY src src
COPY lib lib
COPY settings.gradle settings.gradle
COPY system.properties system.properties

# build application JAR
RUN ./gradlew bootJar
# build justom JRE (use "jdeps -summary ./build/libs/toolbox-server-*.jar" to
# find module dependencies)
RUN jlink \
    --output build/jre \
    --add-modules java.base,java.logging,java.desktop,java.management,java.naming,java.security.jgss,java.instrument \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=2

# Step 2: Install the toolbox + external dependencies to a runner container
FROM debian:bullseye-slim AS runner
WORKDIR /app

COPY scripts scripts

# Install GAMS with conda python environment
ARG GAMS_LICENSE
RUN /app/scripts/setup-gams.sh
ENV GMSPYTHONLIB=/opt/conda/envs/gams/lib/libpython3.10.so
ENV PATH=${PATH}:/opt/conda/bin:/usr/local/bin/gams

# Run all subsequent commands in the GAMS conda env
SHELL ["conda", "run", "-n", "gams", "/bin/bash", "-c"]

# Install custom JRE
COPY --from=builder /app/build/jre /opt/java
ENV PATH="${PATH}:/opt/java/bin"

# Install the toolbox server and its solver scripts
COPY solvers/gams solvers/gams
COPY solvers/qiskit solvers/qiskit
COPY solvers/cirq solvers/cirq
COPY solvers/python solvers/python
COPY solvers/custom solvers/custom
RUN scripts/install-solver-dependencies.sh
COPY --from=builder /app/build/libs/toolbox-server-*.jar toolbox-server.jar

# Run the toolbox server on dokku's default port
EXPOSE 5000
RUN echo "java -jar toolbox-server.jar --server.port=5000" > start.sh
RUN chmod +x start.sh
CMD ["conda", "run", "--no-capture-output", "-n", "gams", "/bin/bash", "-c", "./start.sh"]
