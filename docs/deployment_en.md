# Metrix Deployment Guide

> English | [中文](./deployment.md) · [Back to features](../README_en.md)

This guide covers prerequisites, local development, database initialization and upgrades, Docker deployment, standalone frontend deployment, and configuration and development references. See the [public deployment guide (Chinese)](./public-deployment.md) for server setup, HTTPS, backups, and troubleshooting.

Run commands from the repository root unless a command explicitly changes directories. Local development uses `backend/.env`; Docker deployment uses the root `.env`.

## Tech Stack

| Layer | Technology                                                   |
|-------|--------------------------------------------------------------|
| Backend | Java 21, Spring Boot 4.0.6, MyBatis-Plus 3.5.16              |
| Database | PostgreSQL 16                                                |
| AI | LangChain4j 1.15.0 (OpenAI / Ollama)                         |
| Auth | Sa-Token 1.45.0 (JWT, Annotation-based Auth)                 |
| Frontend | Vue 3, Ant Design Vue, Vite                                  |
| Data Sources | THS Financial Data API (market data), Bocha (news), AKshare (fundamentals) |
| Utilities | Hutool 5.8.44, Lombok                                        |

## Quick Start

### Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 22+
- PostgreSQL 16
- Redis 7+
- Python 3.10+ (for market-data and analysis scripts)

### Local Development

```bash
# 0. Install script dependencies and start PostgreSQL and Redis
pip install -r python-service/requirements.txt
# 1. Configure database
cp backend/.env.example backend/.env
# Edit backend/.env with PostgreSQL, Redis and SMTP settings

# 2. Create and initialize a new database (see upgrade guidance for existing databases)
createdb -h 127.0.0.1 -U postgres stock_analysis
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/ddl-init.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/dml-init.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/system_menu.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f backend/.doc/db/system_api.sql

# 3. Start backend
mvn -f backend/pom.xml clean package -DskipTests
java -jar backend/target/metrix-1.6.1.jar

# 4. Start frontend (new terminal)
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`, backend API at `http://localhost:8080`.

### Upgrading an existing database

Back up the database and apply migration scripts supplied with the target release. Full initialization SQL lives in `backend/.doc/db/` and must only be used for new databases. See the [deployment guide](./public-deployment.md#升级与回滚).

### Docker Deployment

Copy the root environment template and set separate database, Redis and administrator passwords:

```bash
cp .env.example .env
chmod 600 .env
# Set POSTGRES_PASSWORD, REDIS_PASSWORD and ADMIN_PASSWORD in .env.
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
```

Open `http://127.0.0.1:8088`. A fresh database volume is initialized automatically with tables, accounts, menus and API permissions. Existing volumes are preserved. Compose includes Nginx, Java/Python, PostgreSQL and Redis; only the frontend loopback port is published.

For public HTTPS, set `METRIX_DOMAIN` and `ACME_EMAIL` in `.env`, configure DNS and open TCP 80/443, then run:

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --build --wait --wait-timeout 300
```

See the [public deployment guide (Chinese)](./public-deployment.md) for server setup, SMTP, HTTPS, verification, backups, upgrades and troubleshooting.

### Frontend Standalone Deployment (non-Docker)

```bash
cd frontend

# Install dependencies
npm install

# Build for production
npm run build

# Option 1: Deploy with nginx (recommended)
# Copy dist/ to nginx html directory and configure nginx.conf to proxy /api to backend

# Option 2: Vite preview (dev/test only)
npm run preview
```

Frontend runs at `http://localhost:4173` (preview) or your nginx-configured port. The backend API must be reachable.

## Project Structure

```
Metrix/
├── backend/                 # Java Spring Boot backend
│   ├── src/main/java/com/bintech/metrix/
│   ├── src/main/resources/  # Application configuration
│   ├── .doc/db/             # Schema and account/menu/API seed data
│   └── Dockerfile
├── frontend/                # Vue 3 frontend and Nginx configuration
│   └── src/
│       ├── views/
│       ├── components/
│       ├── api/
│       ├── router/
│       ├── composables/
│       └── i18n/
├── python-service/          # Python scripts invoked by the backend
├── deploy/                  # HTTPS and administrator initialization
├── docs/                    # Deployment documentation
│   ├── deployment.md
│   ├── deployment_en.md
│   └── public-deployment.md
├── docker-compose.yml
├── docker-compose.public.yml
└── .env.example             # Docker environment template
```

## Configuration

System configuration via environment variables (supports `.env` file):

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_URL` | `jdbc:postgresql://127.0.0.1:5432/stock_analysis` | Database JDBC URL |
| `POSTGRES_USERNAME` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `postgres` | Database password |

### Authentication, CAPTCHA, and email

See [`backend/.env.example`](../backend/.env.example) for the complete configuration. A user must agree to the Privacy Policy to register; Metrix records the consent and timestamp in `users.privacy_agreed` and `users.privacy_agreed_time`.

- `AUTH_CAPTCHA_ENABLED=true`: CAPTCHA is required for administrator sign-in, user sign-in, and sending email verification codes. Set it to `false` only for local development; both the client and server skip the check.
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `AUTH_MAIL_FROM`: SMTP delivery settings. Verification email delivery uses an isolated executor and the server limits each email address to one request per minute.
- A regular user automatically receives non-administration permissions. Administration routes and `/api/admin/**` are restricted to administrators.

### AI Model Configuration

Manage AI models through the frontend config page:

- **OpenAI-compatible**: API Base URL, API Key, model name
- **Ollama**: Service URL and model name

### Data Source Configuration

- **THS Financial Data API (FUYAO)**: Basic quotes, forward-adjusted daily bars, and current index turnover. Configure `https://fuyao.aicubes.cn` and a new API key in Market Data settings; old provider keys cannot be reused. Turnover rate and volume ratio are unavailable. Historical market turnover still uses Baostock. Docs: https://fuyao.aicubes.cn/docs/
- **Bocha**: News data, requires API Key
- **Instruments**: Configure and enable the THS Financial Data API, then click Sync on the instruments page. A-share names and codes are inserted or updated by full ticker code; unchanged records and existing supplemental fields are preserved. Missing configuration opens a link to settings.

> **Note**: When using AKShare on Windows, if you encounter network proxy errors, the system will automatically clear proxy environment variables (`HTTP_PROXY`/`HTTPS_PROXY`) before launching the Python subprocess to ensure direct internet access.

## API Overview

All APIs are prefixed with `/api`. Some endpoints require authentication (Sa-Token JWT). 100+ API endpoints in total — see `backend/.doc/db/system_api.sql` for the full list.

### Business APIs

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/admin/login` | Administrator sign-in with account, password, and CAPTCHA |
| `POST /api/auth/user/login` | User sign-in with email, password, and CAPTCHA |
| `POST /api/auth/user/register` | User registration (email code and Privacy Policy consent required) |
| `POST /api/auth/user/password/reset` | Reset a user password after email-code verification |
| `POST /api/auth/email-code` | Send a registration or password-reset email code (CAPTCHA and rate limited) |
| `GET /api/auth/captcha` | Get a CAPTCHA |
| `GET /api/auth/verification-config` | Get CAPTCHA enablement status |
| `GET  /api/auth/permissions` | Get current user permissions |
| `POST /api/analysis` | Submit analysis task |
| `GET  /api/analysis` | Analysis records list |
| `GET  /api/analysis/{id}/detail` | Analysis report detail |
| `POST /api/market-review/trigger` | Trigger market review |
| `GET  /api/market-review` | Market review list |
| `POST /api/chat/session` | Create chat session |
| `GET  /api/chat/sessions` | Session list |
| `POST /api/chat/send` | Send message (SSE streaming) |
| `GET  /api/portfolio/holdings` | Holdings list |
| `POST /api/portfolio/holdings/batch` | Batch add holdings |
| `GET  /api/stock-basic/page` | Stock data (paginated) |
| `POST /api/stock-basic/sync` | Incrementally sync A-share instruments using the configured financial data API |
| `GET  /api/stocks/search` | Stock search |

### Admin APIs

| Endpoint | Description | Permission |
|----------|-------------|------------|
| `GET  /api/admin/users` | User list | `system:user:list` |
| `PUT  /api/admin/users/{id}/freeze` | Freeze user | `system:user:freeze` |
| `GET  /api/admin/roles` | Role list | `system:role:list` |
| `POST /api/admin/roles/{id}/menus` | Assign menu permissions | `system:role:assign-menu` |
| `POST /api/admin/roles/{id}/apis` | Assign API permissions | `system:role:assign-api` |
| `GET  /api/admin/menus/tree` | Menu tree | `system:menu:list` |
| `POST /api/admin/menus` | Create menu | `system:menu:create` |
| `GET  /api/admin/apis` | API list | `system:api:list` |

## Development Guide

- Use MyBatis-Plus `LambdaQueryWrapper` / `LambdaUpdateWrapper` for DB operations, no raw SQL
- Entity classes must extend `BaseEntity` with Lombok annotations
- Controllers handle only request validation and delegation; services access data through DAOs, and only DAO implementations use mappers
- Use constructor injection, never `@Autowired`
- Database migration scripts go in `backend/.doc/db/`
- See the [repository](../AGENTS.md), [backend](../backend/AGENTS.md), and [frontend](../frontend/AGENTS.md) guidelines for the complete development rules
