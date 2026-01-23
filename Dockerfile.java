# ===========================================
# Java GUI Application Container
# ===========================================
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy Maven project files
COPY language_proximity/pom.xml ./pom.xml
COPY language_proximity/src/ ./src/

# Download dependencies first (cached layer)
RUN mvn dependency:go-offline

# Build the application with all dependencies
RUN mvn clean package -DskipTests

# Create fat JAR with dependencies
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/lib


# ===========================================
# Runtime Stage
# ===========================================
FROM eclipse-temurin:17-jre

# Install X11 libraries for GUI
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxrender1 \
    libxtst6 \
    libxi6 \
    libxext6 \
    libx11-6 \
    fontconfig \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy built JAR and dependencies
COPY --from=builder /app/target/*.jar ./app.jar
COPY --from=builder /app/target/lib/ ./lib/

# Copy data directory
COPY data/ ./data/

# Set display for X11 (needs to be provided at runtime)
ENV DISPLAY=:0

# Run the Java application
# Note: For GUI to work, you need X11 forwarding set up
ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "com.language_proximity.App"]
