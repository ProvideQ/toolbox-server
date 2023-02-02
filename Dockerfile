# Step 1: Build the toolbox-server Java application
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Install dependencies first
COPY gradle gradle
COPY gradlew gradlew
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

# TODO: Install GAMS + Python

# Install the toolbox server
COPY --from=builder /app/build/libs/toolbox-server-0.0.1-SNAPSHOT.jar toolbox-server.jar

# Run the toolbox server on dokku's default port
EXPOSE 5000
CMD ["java", "-jar", "toolbox-server.jar", "--server.port=5000"]
