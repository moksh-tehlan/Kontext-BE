# 🐳 Docker Setup for Kontext

## Quick Start

### Development Environment
```bash
# Start all services
docker-compose up -d

# Start with tools (pgAdmin, Redis Commander)
docker-compose --profile tools up -d

# View logs
docker-compose logs -f app

# Stop services
docker-compose down
```

### Production Environment
```bash
# Copy environment template
cp .env.example .env

# Edit .env with your production values
vim .env

# Build and start production services
docker-compose -f docker-compose.prod.yaml up -d

# Check status
docker-compose -f docker-compose.prod.yaml ps
```

## 🌐 Service Ports

| Service | Port | Description |
|---------|------|-------------|
| Kontext App | 8080 | Main application |
| Management | 8081 | Actuator endpoints |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |
| Redis Commander | 8082 | Redis GUI (dev only) |
| pgAdmin | 8083 | PostgreSQL GUI (dev only) |

## 🔧 Configuration Files

- `docker/postgres/init.sql` - PostgreSQL initialization
- `docker/redis/redis.conf` - Redis configuration
- `.env` - Environment variables (create from .env.example)

## 🏗️ Build Application

```bash
# Build image
docker build -t kontext:latest .

# Build with specific tag
docker build -t kontext:v1.0.0 .
```

## 📊 Monitoring

### Health Checks
```bash
# App health
curl http://localhost:8081/actuator/health

# Database health
docker-compose exec postgres pg_isready

# Redis health
docker-compose exec redis redis-cli ping
```

### Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f app
docker-compose logs -f postgres
docker-compose logs -f redis
```

## 🔒 Security Notes

1. **Change default passwords** in production
2. **Use environment variables** for sensitive data
3. **Enable SSL/TLS** for production
4. **Restrict network access** appropriately
5. **Regular backups** of PostgreSQL data

## 🚀 Production Deployment

1. Set up environment variables in `.env`
2. Configure reverse proxy (Nginx)
3. Set up SSL certificates
4. Configure monitoring and logging
5. Set up automated backups

## 🛠️ Troubleshooting

### Common Issues

**Container won't start:**
```bash
docker-compose logs [service-name]
```

**Database connection issues:**
```bash
docker-compose exec app ping postgres
```

**Redis connection issues:**
```bash
docker-compose exec app ping redis
```

**Clean restart:**
```bash
docker-compose down -v
docker-compose up -d
```