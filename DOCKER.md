# Language Proximity Analysis - Docker Setup

This document describes how to run the Language Proximity Analysis project using Docker.

## Prerequisites

- **Docker** (v20.10+): https://docs.docker.com/get-docker/
- **Docker Compose** (v2.0+): Usually included with Docker Desktop

## Project Components

| Component | Technology | Purpose |
|-----------|------------|---------|
| **Python Analysis** | Python 3.11 | Calculates language proximity using Levenshtein distance |
| **Java GUI** | Java 17 + Maven | Visual interface for exploring results |

## Quick Start

### 1. Run Python Analysis (Recommended)

```bash
# Build and run the analysis
docker-compose up python-analysis

# Or build first, then run
docker-compose build python-analysis
docker-compose run python-analysis
```

This will:
- Build the Python container with all dependencies (pandas, igraph, leidenalg, etc.)
- Mount the `./data` directory
- Run `compute_proximity.py`
- Output results to `./data/` folder

### 2. Run Specific Python Scripts

```bash
# Run translation script
docker-compose run python-analysis python python_scripts/translate_words.py

# Run IPA conversion
docker-compose run python-analysis python python_scripts/convert_to_ipa.py

# Run CSV cleaning
docker-compose run python-analysis python python_scripts/clean_csv.py
```

### 3. Interactive Development Shell

```bash
# Start a bash shell in the container
docker-compose --profile dev run dev

# Inside the container:
python python_scripts/compute_proximity.py
```

## Building Images Separately

```bash
# Build Python analysis image
docker build -t language-proximity-python --target python-runtime .

# Build Java GUI image (experimental)
docker build -t language-proximity-java -f Dockerfile.java .
```

## Java GUI (Experimental)

The Java GUI application requires X11 forwarding to display the graphical interface.

### On Linux:
```bash
xhost +local:docker
docker-compose --profile gui up java-gui
```

### On Windows (with VcXsrv):
1. Install VcXsrv: https://sourceforge.net/projects/vcxsrv/
2. Start VcXsrv with "Disable access control" checked
3. Set DISPLAY environment variable:
```powershell
$env:DISPLAY="host.docker.internal:0"
docker-compose --profile gui up java-gui
```

### On macOS (with XQuartz):
1. Install XQuartz: https://www.xquartz.org/
2. Start XQuartz with "Allow connections from network clients" enabled
3. Run:
```bash
xhost +localhost
export DISPLAY=host.docker.internal:0
docker-compose --profile gui up java-gui
```

## Volume Mounts

| Host Path | Container Path | Purpose |
|-----------|---------------|---------|
| `./data` | `/app/data` | Input CSV files and output results |
| `./python_scripts` (dev only) | `/app/python_scripts` | Live code editing |

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PYTHONUNBUFFERED` | `1` | Show Python output immediately |
| `DISPLAY` | `:0` | X11 display for Java GUI |

## Cleaning Up

```bash
# Stop all containers
docker-compose down

# Remove all containers and images
docker-compose down --rmi all

# Remove unused Docker resources
docker system prune -a
```

## Troubleshooting

### "No module named 'pandas'"
The container build may have failed. Rebuild with:
```bash
docker-compose build --no-cache python-analysis
```

### "igraph/leidenalg installation failed"
These libraries require C compilers. Ensure you're using the provided Dockerfile which includes build tools.

### Java GUI not showing
X11 forwarding is complex in Docker. Consider running the Java app natively instead:
```bash
cd language_proximity
mvn clean compile exec:java -Dexec.mainClass="com.language_proximity.App"
```

### Data not persisting
Ensure you're using the volume mount:
```bash
docker-compose run -v $(pwd)/data:/app/data python-analysis
```

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Docker Compose                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌─────────────────┐    ┌─────────────────┐                 │
│  │ python-analysis │    │    java-gui     │                 │
│  │   (Python 3.11) │    │    (Java 17)    │                 │
│  │                 │    │                 │                 │
│  │ • pandas        │    │ • GraphStream   │                 │
│  │ • igraph        │    │ • FlatLaf       │                 │
│  │ • leidenalg     │    │ • Swing UI      │                 │
│  │ • Levenshtein   │    │                 │                 │
│  └────────┬────────┘    └────────┬────────┘                 │
│           │                      │                          │
│           └──────────┬───────────┘                          │
│                      │                                      │
│              ┌───────▼───────┐                              │
│              │   ./data/     │  (Volume Mount)              │
│              │               │                              │
│              │ • CSV files   │                              │
│              │ • Results     │                              │
│              └───────────────┘                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```
