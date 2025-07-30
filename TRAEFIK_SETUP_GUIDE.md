# Traefik Implementation Guide

This guide provides step-by-step instructions for implementing Traefik as a reverse proxy in your Docker Compose setup.

## Table of Contents

1. [What is Traefik?](#what-is-traefik)
2. [Prerequisites](#prerequisites)
3. [Step-by-Step Implementation](#step-by-step-implementation)
4. [Configuration Examples](#configuration-examples)
5. [Advanced Features](#advanced-features)
6. [Troubleshooting](#troubleshooting)
7. [Best Practices](#best-practices)

## What is Traefik?

Traefik is a modern reverse proxy and load balancer that makes deploying microservices easy. It automatically discovers services and creates routing rules based on labels or configuration files.

### Key Benefits:
- **Automatic Service Discovery**: Detects Docker containers automatically
- **Dynamic Configuration**: Updates routing without restarts
- **Single Entry Point**: All services accessible through one port
- **Built-in Load Balancing**: Distributes traffic across service instances
- **SSL/TLS Termination**: Handles HTTPS certificates automatically
- **Web Dashboard**: Visual interface for monitoring routes and services

## Prerequisites

- Docker and Docker Compose installed
- Basic understanding of Docker networking
- Services containerized and ready to deploy

## Step-by-Step Implementation

### Step 1: Create Traefik Network

First, create a dedicated Docker network for Traefik:

```yaml
networks:
  traefik:
    external: false
```

### Step 2: Configure Traefik Service

Add the Traefik service to your `compose.yml`:

```yaml
services:
  traefik:
    image: traefik:v3.0
    command:
      - --entrypoints.web.address=:80                    # HTTP entry point
      - --providers.docker=true                          # Enable Docker provider
      - --providers.docker.exposedbydefault=false        # Only expose labeled services
      - --api.dashboard=true                              # Enable dashboard
      - --api.insecure=true                              # Allow insecure dashboard access
    ports:
      - "80:80"        # HTTP traffic
      - "8081:8080"    # Dashboard port
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock       # Docker socket access
    networks:
      - traefik
    restart: unless-stopped
```

### Step 3: Configure Your Services

Add Traefik labels to each service you want to expose:

#### Backend Service Example:
```yaml
  server:
    build:
      context: ./server
      dockerfile: Dockerfile
    labels:
      - "traefik.enable=true"                                              # Enable Traefik
      - "traefik.http.routers.server.rule=Host(`localhost`) && PathPrefix(`/api`)"  # Routing rule
      - "traefik.http.routers.server.entrypoints=web"                     # Use web entry point
      - "traefik.http.routers.server.priority=100"                        # Higher priority for specific paths
      - "traefik.http.services.server.loadbalancer.server.port=8080"      # Internal service port
    networks:
      - traefik
```

#### Frontend Service Example:
```yaml
  client:
    build:
      context: ./client
      dockerfile: Dockerfile
    labels:
      - "traefik.enable=true"                                  # Enable Traefik
      - "traefik.http.routers.client.rule=Host(`localhost`)"   # Catch-all rule
      - "traefik.http.routers.client.entrypoints=web"          # Use web entry point
      - "traefik.http.services.client.loadbalancer.server.port=3000"  # Internal service port
    networks:
      - traefik
```

### Step 4: Connect All Services to Traefik Network

Ensure all services that need to communicate are on the traefik network:

```yaml
  database:
    image: postgres:17
    networks:
      - traefik
    # ... other configuration
```

## Configuration Examples

### Multi-Domain Setup

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.app.rule=Host(`app.example.com`)"
  - "traefik.http.routers.api.rule=Host(`api.example.com`)"
```

### Path-Based Routing

```yaml
labels:
  - "traefik.enable=true"
  - "traefik.http.routers.admin.rule=Host(`localhost`) && PathPrefix(`/admin`)"
  - "traefik.http.routers.admin.priority=200"
```

### HTTPS with Let's Encrypt

```yaml
traefik:
  command:
    - --entrypoints.web.address=:80
    - --entrypoints.websecure.address=:443
    - --certificatesresolvers.letsencrypt.acme.email=your-email@domain.com
    - --certificatesresolvers.letsencrypt.acme.storage=/acme.json
    - --certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web
  volumes:
    - ./acme.json:/acme.json
```

Service with HTTPS:
```yaml
labels:
  - "traefik.http.routers.app.tls.certresolver=letsencrypt"
  - "traefik.http.routers.app.entrypoints=websecure"
```

### Load Balancing Multiple Instances

```yaml
  api:
    deploy:
      replicas: 3
    labels:
      - "traefik.enable=true"
      - "traefik.http.routers.api.rule=Host(`api.localhost`)"
      - "traefik.http.services.api.loadbalancer.server.port=8080"
```

## Advanced Features

### Middleware Configuration

#### Basic Authentication:
```yaml
labels:
  - "traefik.http.middlewares.auth.basicauth.users=admin:$$2y$$10$$..."
  - "traefik.http.routers.admin.middlewares=auth"
```

#### CORS Headers:
```yaml
labels:
  - "traefik.http.middlewares.cors.headers.accesscontrolalloworigin=*"
  - "traefik.http.routers.api.middlewares=cors"
```

#### Rate Limiting:
```yaml
labels:
  - "traefik.http.middlewares.ratelimit.ratelimit.burst=100"
  - "traefik.http.middlewares.ratelimit.ratelimit.average=50"
  - "traefik.http.routers.api.middlewares=ratelimit"
```

### Health Checks Integration

```yaml
  server:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    labels:
      - "traefik.http.services.server.loadbalancer.healthcheck.path=/health"
      - "traefik.http.services.server.loadbalancer.healthcheck.interval=30s"
```

## Troubleshooting

### Common Issues and Solutions

#### 1. Service Not Accessible
**Problem**: Service returns 404 or connection refused

**Solutions**:
- Check if service has `traefik.enable=true` label
- Verify the routing rule syntax
- Ensure service is on the traefik network
- Check if the internal port is correct

```bash
# Debug commands
docker compose logs traefik
curl -s http://localhost:8081/api/http/routers | jq
```

#### 2. Dashboard Not Loading
**Problem**: Traefik dashboard shows blank or inaccessible

**Solutions**:
- Verify port mapping: `"8081:8080"`
- Check `--api.dashboard=true` and `--api.insecure=true` flags
- Access via correct URL: `http://localhost:8081`

#### 3. Priority Conflicts
**Problem**: Wrong service responding to requests

**Solutions**:
- Set explicit priorities: higher numbers = higher priority
- Use more specific routing rules
- Check rule syntax and conflicts

```yaml
labels:
  - "traefik.http.routers.api.priority=100"    # Higher priority
  - "traefik.http.routers.web.priority=50"     # Lower priority
```

#### 4. Docker Socket Permission Issues
**Problem**: Traefik can't discover services

**Solutions**:
- Ensure Docker socket is mounted: `/var/run/docker.sock:/var/run/docker.sock`
- Check Docker socket permissions
- On some systems, add user to docker group

### Debugging Commands

```bash
# Check Traefik logs
docker compose logs traefik

# View active routers
curl http://localhost:8081/api/http/routers

# View services
curl http://localhost:8081/api/http/services

# Test routing
curl -H "Host: localhost" http://localhost/api/health

# Check Docker networks
docker network ls
docker network inspect <network-name>
```

## Best Practices

### Security
1. **Disable Insecure API**: Remove `--api.insecure=true` in production
2. **Use HTTPS**: Enable TLS for production deployments
3. **Limit Dashboard Access**: Use basic auth or IP restrictions
4. **Regular Updates**: Keep Traefik version updated

### Performance
1. **Resource Limits**: Set appropriate CPU/memory limits
2. **Health Checks**: Configure proper health check endpoints
3. **Connection Pooling**: Use appropriate load balancer settings

### Monitoring
1. **Enable Metrics**: Use Prometheus/monitoring endpoints
2. **Log Analysis**: Centralize and analyze Traefik logs
3. **Dashboard Monitoring**: Regular checks of service status

### Configuration Management
1. **Environment Variables**: Use environment-specific configurations
2. **External Configuration**: Consider file-based configuration for complex setups
3. **Documentation**: Keep routing rules documented and updated

### Example Production Configuration

```yaml
traefik:
  image: traefik:v3.0
  command:
    - --entrypoints.web.address=:80
    - --entrypoints.websecure.address=:443
    - --providers.docker=true
    - --providers.docker.exposedbydefault=false
    - --certificatesresolvers.letsencrypt.acme.email=admin@company.com
    - --certificatesresolvers.letsencrypt.acme.storage=/acme.json
    - --certificatesresolvers.letsencrypt.acme.httpchallenge.entrypoint=web
    - --api.dashboard=true
    - --metrics.prometheus=true
    - --log.level=INFO
    - --accesslog=true
  ports:
    - "80:80"
    - "443:443"
  volumes:
    - /var/run/docker.sock:/var/run/docker.sock:ro
    - ./acme.json:/acme.json
  networks:
    - traefik
  restart: unless-stopped
  labels:
    # Dashboard with basic auth
    - "traefik.enable=true"
    - "traefik.http.routers.dashboard.rule=Host(`traefik.company.com`)"
    - "traefik.http.routers.dashboard.tls.certresolver=letsencrypt"
    - "traefik.http.routers.dashboard.middlewares=auth"
    - "traefik.http.middlewares.auth.basicauth.users=admin:$$2y$$10$$..."
```

This guide covers the complete implementation of Traefik in your project. Start with the basic setup and gradually add advanced features as needed.