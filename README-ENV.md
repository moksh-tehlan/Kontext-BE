# Environment Configuration

This project uses environment-specific configuration files to manage different deployment environments.

## Environment Files

### Development
- Copy `.env.example` to `.env.dev`
- Update the values with your development configuration
- This file is already created with default development values

### Production
- Copy `.env.prod.example` to `.env.prod`
- Update the values with your production configuration
- **Never commit `.env.prod` to version control**

## Required Environment Variables

| Variable | Description | Development Default | Production Required |
|----------|-------------|-------------------|-------------------|
| `DB_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/kontext_dev` | ✅ |
| `DB_USERNAME` | Database username | `kontext_user` | ✅ |
| `DB_PASSWORD` | Database password | `kontext_password` | ✅ |
| `OPENAI_API_KEY` | OpenAI API key | `your-dev-openai-api-key-here` | ✅ |
| `SERVER_PORT` | Application server port | `8080` | Optional |
| `MANAGEMENT_PORT` | Management/actuator port | `8081` | Optional |
| `ADMIN_USERNAME` | Admin username | `admin` | ✅ |
| `ADMIN_PASSWORD` | Admin password | `admin123` | ✅ |
| `ALLOWED_ORIGINS` | CORS allowed origins | `http://localhost:3000,http://localhost:4200` | ✅ |
| `SSL_KEYSTORE_PATH` | SSL keystore path | - | Optional |
| `SSL_KEYSTORE_PASSWORD` | SSL keystore password | - | Optional |

## Usage

### Development
```bash
# Set active profile to dev (default)
export SPRING_PROFILES_ACTIVE=dev

# Or specify in IDE/run configuration
-Dspring.profiles.active=dev
```

### Production
```bash
# Set active profile to prod
export SPRING_PROFILES_ACTIVE=prod

# Load environment variables from file
export $(cat .env.prod | xargs)

# Or specify in run configuration
-Dspring.profiles.active=prod
```

## Security Notes

- Never commit actual environment files (`.env`, `.env.dev`, `.env.prod`) to version control
- Use strong passwords in production
- Rotate API keys regularly
- Use environment variable injection in production deployments (Docker, Kubernetes, etc.)