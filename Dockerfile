# ===========================================
# Python Analysis Container
# ===========================================
FROM python:3.11-slim AS python-base

# Install system dependencies for igraph and leidenalg
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    libxml2-dev \
    zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Python requirements and install
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy Python scripts
COPY python_scripts/ ./python_scripts/

# Copy data directory
COPY data/ ./data/

# Default command runs the proximity analysis
CMD ["python", "python_scripts/compute_proximity.py"]


# ===========================================
# Java GUI Container (for development/building)
# ===========================================
FROM maven:3.9-eclipse-temurin-17 AS java-builder

WORKDIR /app

# Copy Maven project files
COPY language_proximity/pom.xml ./language_proximity/
COPY language_proximity/src/ ./language_proximity/src/

# Build the Java application
WORKDIR /app/language_proximity
RUN mvn clean package -DskipTests


# ===========================================
# Final Python Runtime Container
# ===========================================
FROM python:3.11-slim AS python-runtime

# Install runtime dependencies for igraph
RUN apt-get update && apt-get install -y --no-install-recommends \
    libxml2 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy installed packages from builder
COPY --from=python-base /usr/local/lib/python3.11/site-packages /usr/local/lib/python3.11/site-packages

# Copy application code
COPY python_scripts/ ./python_scripts/
COPY data/ ./data/

# Create volume mount point for data persistence
VOLUME ["/app/data"]

# Set environment variables
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

# Run proximity analysis
CMD ["python", "python_scripts/compute_proximity.py"]
