<img src=".doc/images/Metrix.png" alt="Metrix" width="300">

# Metrix

> English | [中文](./README.md)

Metrix = Metric + Matrix — an asset evaluation tool from the perspective of quantitative trading.

## Web Preview
![Analysis - AI multi-dimensional analysis page](.doc/images/analysis_img_en.png)
<p align="center">AI multi-dimensional analysis page</p>

![Review - AI daily market review page](.doc/images/market_review_en.png)
<p align="center">AI daily market review page</p>

![Chat - Multi-turn conversational AI analysis page](.doc/images/chat_en.png)
<p align="center">Multi-turn conversational AI analysis page</p>

## Tech Stack

| Layer | Technology                                                   |
|-------|--------------------------------------------------------------|
| Backend | Java 21, Spring Boot 4.0.5, MyBatis-Plus 3.5.15              |
| Database | PostgreSQL 16                                                |
| AI | LangChain4j 0.36.2 (OpenAI / Ollama)                         |
| Auth | Sa-Token 1.45.0 (JWT, Annotation-based Auth)                 |
| Frontend | Vue 3, Ant Design Vue, Vite                                  |
| Data Sources | TickFlow (market data), Bocha (news), AKshare (fundamentals) |
| Utilities | Hutool 5.8.13, Flexmark, Lombok                              |

## Features

- **AI Analysis** — Multi-dimensional stock analysis powered by LLMs (technical analysis, capital flow, news sentiment)
- **AI Chat** — Multi-turn conversational analysis with real-time tracking of 8-step processing (timing & status), Markdown streaming rendering
- **Theme Switching** — 5 built-in color themes (Sky Blue / Emerald Green / Twilight Purple / Sunset Orange / Aurora Cyan), globally applied via Ant Design Vue Design Tokens
- **Real-time Market Data** — Real-time quotes, depth data, and K-line data via TickFlow
- **News Aggregation** — Stock-related news via Bocha API with auto-summarization
- **Chip Distribution** — Python-based shareholding cost distribution analysis
- **PDF Export** — Export analysis reports as PDF
- **Market Review** — AI-powered daily review of major A-share indices with market summary and trend analysis
- **Portfolio Management** — Multi-account holding management with batch entry, one-click evaluation, and real-time P&L monitoring
- **Feishu Notifications** — Push analysis results via Feishu Webhook
- **Multi-Model Support** — OpenAI-compatible APIs and local Ollama models
- **i18n** — Chinese and English UI
- **Permission Management** — RBAC-based permission system with role/menu/API three-level access control, annotation-based API authorization, and dynamic menu rendering

## Quick Start

### Prerequisites

- JDK 21
- Maven 3.9+
- Node.js 18+
- PostgreSQL 16
- Python 3.10+ (optional, for chip analysis)

### Local Development

```bash
# 1. Configure database
cp .env.example .env
# Edit .env with your PostgreSQL connection info

# 2. Initialize database
psql -h 127.0.0.1 -U postgres -d stock_analysis -f .doc/db/ddl-init.sql
psql -h 127.0.0.1 -U postgres -d stock_analysis -f .doc/db/dml-init.sql

# 3. Start backend
mvn clean package -DskipTests
java -jar target/stock-analysis-1.0.0.jar

# 4. Start frontend (new terminal)
cd frontend
npm install
npm run dev
```

Frontend runs at `http://localhost:5173`, backend API at `http://localhost:8080`.

### Docker Deployment

```bash
# Build and start all services (backend + frontend + database)
docker compose up -d --build

# Initialize database (first deployment only)
docker compose exec -T postgres psql -U postgres -d stock_analysis < .doc/db/ddl-init.sql
docker compose exec -T postgres psql -U postgres -d stock_analysis < .doc/db/dml-init.sql

# View logs
docker compose logs -f app
```

Access `http://localhost` to use the system.

> **Architecture note**: In production, Nginx serves the Vue SPA static files and reverse-proxies `/api` requests to the Java backend, eliminating the need for CORS configuration.

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
├── src/main/java/com/bintech/metrix/
│   ├── controller/        # REST controllers
│   │   └── admin/         # Admin console controllers
│   ├── service/           # Business logic layer
│   ├── repository/        # Persistence layer (Entity + Mapper)
│   ├── dto/               # Data transfer objects
│   ├── enums/             # Enumerations
│   ├── config/            # Configuration classes (incl. Sa-Token auth)
│   ├── constants/         # Constants
│   └── exception/         # Global exception handler
├── frontend/              # Vue 3 frontend
│   └── src/
│       ├── views/         # Page components
│       │   └── admin/     # Admin console pages
│       ├── api/           # API client
│       ├── router/        # Router configuration
│       ├── composables/   # Vue composables (themes, state management, etc.)
│       └── i18n/          # Internationalization
├── python-service/        # Python sidecar services
└── .doc/
    ├── db/                # Database scripts (DDL + DML + seed data)
    │   ├── ddl-init.sql   # Full table creation script
    │   ├── dml-init.sql   # Initial data (roles, admin user)
    │   └── system_api.sql # API registration data
    └── images/            # Image assets
```

## Configuration

System configuration via environment variables (supports `.env` file):

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_URL` | `jdbc:postgresql://127.0.0.1:5432/stock_analysis` | Database JDBC URL |
| `POSTGRES_USERNAME` | `postgres` | Database username |
| `POSTGRES_PASSWORD` | `postgres` | Database password |

### AI Model Configuration

Manage AI models through the frontend config page:
- **OpenAI-compatible**: API Base URL, API Key, model name
- **Ollama**: Service URL and model name

### Data Source Configuration

- **TickFlow**: Real-time market data, requires API Key
- **Bocha**: News data, requires API Key
- **Tushare**: Stock fundamentals, import via CSV

> **Note**: When using AKShare on Windows, if you encounter network proxy errors, the system will automatically clear proxy environment variables (`HTTP_PROXY`/`HTTPS_PROXY`) before launching the Python subprocess to ensure direct internet access.

## API Overview

All APIs are prefixed with `/api`. Some endpoints require authentication (Sa-Token JWT). 100+ API endpoints in total — see `.doc/db/system_api.sql` for the full list.

### Business APIs

| Endpoint | Description |
|----------|-------------|
| `POST /api/auth/login` | User login |
| `POST /api/auth/login-by-code` | WeChat scan-code login |
| `GET  /api/auth/permissions` | Get current user permissions |
| `POST /api/analysis` | Submit analysis task |
| `GET  /api/analysis` | Analysis records list |
| `GET  /api/analysis/{id}/detail` | Analysis report detail |
| `GET  /api/analysis/{id}/pdf` | Export PDF |
| `POST /api/market-review/trigger` | Trigger market review |
| `GET  /api/market-review` | Market review list |
| `POST /api/chat/session` | Create chat session |
| `GET  /api/chat/sessions` | Session list |
| `POST /api/chat/send` | Send message (SSE streaming) |
| `GET  /api/portfolio/holdings` | Holdings list |
| `POST /api/portfolio/holdings/batch` | Batch add holdings |
| `GET  /api/stock-basic/page` | Stock data (paginated) |
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
| `GET  /api/admin/usage-stats/today` | Today's stats | `system:stats:today` |
| `GET  /api/admin/audit-logs` | Audit logs | `system:audit:view` |

## Development Guide

- Use MyBatis-Plus `LambdaQueryWrapper` / `LambdaUpdateWrapper` for DB operations, no raw SQL
- Entity classes must extend `BaseEntity` with Lombok annotations
- Controllers handle only request validation and delegation
- Use constructor injection, never `@Autowired`
- Database migration scripts go in `.doc/db/`

## License

MIT License | Copyright © 2026 bin.li
