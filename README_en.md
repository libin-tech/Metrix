# Metrix

> English | [中文](./README.md)

Metrix = Metric + Matrix — an asset evaluation tool from the perspective of quantitative trading.

## Features

- **AI Analysis** — Multi-dimensional stock analysis powered by LLMs (technical analysis, capital flow, news sentiment)
- **AI Chat** — Multi-turn conversational analysis with real-time tracking of 8-step processing (timing & status), Markdown streaming rendering
- **Theme Switching** — 5 built-in color themes (Sky Blue / Emerald Green / Twilight Purple / Sunset Orange / Aurora Cyan), applied throughout the interface
- **Real-time Market Data** — Basic quotes and forward-adjusted daily bars via the THS Financial Data API; MACD is calculated locally
- **News Aggregation** — Stock-related news via Bocha API with auto-summarization
- **Chip Distribution** — Latest trading-day chip metrics via AKShare (forward-adjusted), including profit ratio, cost ranges, and concentration; no market-data API key required
- **Market Review** — AI-powered daily review of major A-share indices with market summary and trend analysis
- **Portfolio Management** — Multi-account holding management with batch entry, one-click evaluation, and real-time P&L monitoring
- **Feishu Notifications** — Push analysis results via Feishu Webhook
- **Multi-Model Support** — OpenAI-compatible APIs and local Ollama models
- **i18n** — Chinese and English UI
- **Permission Management** — RBAC-based permission system with role/menu/API three-level access control and dynamic menu rendering
- **Accounts and verification** — Administrators sign in with an account, password, and CAPTCHA; users register and sign in by email, with password reset and Privacy Policy consent

For deployment and configuration, see the [deployment guide](./docs/deployment_en.md).

## License

MIT License | Copyright © 2026 bin.li
