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

RUN apt-get update && apt-get install curl --yes

# GAMS Installation script is based on the official installation guide
# (https://www.gams.com/latest/docs/UG_UNIX_INSTALL.html) and adapts some lines from
# iisaa/gams-docker (https://github.com/iiasa/gams-docker/blob/master/Dockerfile, GPL-3.0 licensed)

# Download GAMS
ARG GAMS_LICENSE
ENV GAMS_VERSION_RELEASE_MAJOR=42.1
ENV GAMS_VERSION_HOTFIX=0
RUN curl --show-error --output /opt/gams/gams.exe --create-dirs "https://d37drm4t2jghv5.cloudfront.net/distributions/${GAMS_VERSION_RELEASE_MAJOR}.${GAMS_VERSION_HOTFIX}/linux/linux_x64_64_sfx.exe" &&\
    # Extract GAMS files
    cd /opt/gams &&\
    chmod +x gams.exe &&\
    sync &&\
    # -q = quietly, see https://linux.die.net/man/1/unzipsfx
    ./gams.exe -q &&\
    rm -rf gams.exe &&\
    # Install GAMS license
    echo "${GAMS_LICENSE}" | base64 --decode > /opt/gams/gams${GAMS_VERSION_RELEASE_MAJOR}_linux_x64_64_sfx/gamslice.txt &&\
    # Add Path and run GAMS Installer
    GAMS_PATH=$(dirname $(find / -name gams -type f -executable -print)) &&\
    ln -s $GAMS_PATH/gams /usr/local/bin/gams &&\
    echo "export PATH=\$PATH:$GAMS_PATH" >> ~/.bashrc &&\
    cd $GAMS_PATH &&\
    ./gamsinst -a

# Install python from miniconda (with python -> python3 alias) and pip
# note about install.sh: "-b" = non-interactive batch mode, "-p /opt/conda" = installation directory
RUN curl --show-error --output /opt/conda-installer/install.sh --create-dirs "https://repo.anaconda.com/miniconda/Miniconda3-py310_23.1.0-1-Linux-x86_64.sh" &&\
    cd /opt/conda-installer &&\
    echo "32d73e1bc33fda089d7cd9ef4c1be542616bd8e437d1f77afeeaf7afdb019787 install.sh" | sha256sum --check &&\
    chmod +x ./install.sh &&\
    ./install.sh -b -p /opt/conda &&\
    cd /app &&\
    rm --recursive /opt/conda-installer
ENV PATH="${PATH}:/opt/conda/bin"
RUN conda create --name gams python=3.10 --yes
ENV GMSPYTHONLIB=/opt/conda/envs/gams/lib/libpython3.10.so
SHELL ["conda", "run", "-n", "gams", "/bin/bash", "-c"]
RUN pip install gams[core,connect] --find-links /opt/gams/gams${GAMS_VERSION_RELEASE_MAJOR}_linux_x64_64_sfx/api/python/bdist

# Install custom JRE
COPY --from=builder /app/build/jre /opt/java
ENV PATH="${PATH}:/opt/java/bin"

# Install the toolbox server and its solver scripts
COPY gams gams
RUN pip install -r ./gams/requirements.txt
COPY qiskit qiskit
RUN pip install -r ./qiskit/requirements.txt
COPY --from=builder /app/build/libs/toolbox-server-0.0.1-SNAPSHOT.jar toolbox-server.jar

# Run the toolbox server on dokku's default port
EXPOSE 5000
RUN echo "java -jar toolbox-server.jar --server.port=5000" > start.sh
RUN chmod +x start.sh
CMD ["conda", "run", "--no-capture-output", "-n", "gams", "/bin/bash", "-c", "./start.sh"]
