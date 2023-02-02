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
COPY settings.gradle settings.gradle
COPY system.properties system.properties

RUN ./gradlew bootJar

# Step 2: Install the toolbox + external dependencies to a runner container
FROM eclipse-temurin:17-jdk-jammy AS runner
WORKDIR /app

# Install python (with python -> python3 alias)
RUN apt update
RUN apt-get install python-is-python3 --yes

# Download GAMS
RUN curl --show-error --output /opt/gams/gams.exe --create-dirs "https://d37drm4t2jghv5.cloudfront.net/distributions/41.5.0/linux/linux_x64_64_sfx.exe"

# Extract GAMS files
RUN cd /opt/gams && chmod +x gams.exe; sync && ./gams.exe && rm -rf gams.exe

# Install GAMS license
ARG GAMS_LICENSE
RUN echo "${GAMS_LICENSE}" | base64 --decode > /opt/gams/gams41.5_linux_x64_64_sfx/gamslice.txt

# Add Path and run GAMS Installer
RUN GAMS_PATH=$(dirname $(find / -name gams -type f -executable -print)) &&\
    ln -s $GAMS_PATH/gams /usr/local/bin/gams &&\
    echo "export PATH=\$PATH:$GAMS_PATH" >> ~/.bashrc &&\
    cd $GAMS_PATH &&\
    ./gamsinst -a

# Install the toolbox server and its GAMS scripts
COPY gams gams
COPY qiskit qiskit
COPY --from=builder /app/build/libs/toolbox-server-0.0.1-SNAPSHOT.jar toolbox-server.jar

# Run the toolbox server on dokku's default port
EXPOSE 5000
CMD ["java", "-jar", "toolbox-server.jar", "--server.port=5000"]
