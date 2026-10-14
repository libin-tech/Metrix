# Metrix 公网部署指南

本指南使用一台 Linux 服务器运行 Metrix 前端、Java 后端、Python 行情脚本、PostgreSQL 和 Redis，并通过 Caddy 提供域名 HTTPS 访问。基础 Compose 仅在服务器回环地址开放 HTTP，公网入口由单独的 Compose 配置启用。

以下命令均从仓库根目录执行，安装软件和准备目录的命令除外。首次部署使用空数据库；已有部署请先阅读「升级与回滚」和「已有数据库」。

## 部署结构

```text
浏览器 → Caddy 80/443（HTTPS、证书续期）
              → frontend:80（Nginx、Vue 静态文件）
                  → app:8080（/api、SSE、WebSocket、Python 子进程）
                      → postgres:5432
                      → redis:6379
```

PostgreSQL、Redis 和 Java 后端没有宿主机端口映射。Nginx 的宿主机入口为 `127.0.0.1:8088`，用于本机检查或 SSH 隧道。健康检查接口 `/actuator/health` 仅在后端内部可用，Nginx 不向公网转发。

| 文件 | 用途 |
|---|---|
| `docker-compose.yml` | 基础服务、持久化、健康检查和数据库初始化 |
| `docker-compose.public.yml` | 追加公网 Caddy 服务和 80/443 端口 |
| `.env.example` | Docker 部署变量模板 |
| `backend/Dockerfile` | Java 21 和 Python 运行环境，从仓库根目录构建 |
| `frontend/Dockerfile` | Node.js 22 构建 Vue，Nginx 提供生产服务 |
| `frontend/nginx.conf` | 同源 API、SSE 流式响应和 WebSocket 转发 |
| `deploy/Caddyfile` | 域名 HTTPS 和自动证书管理 |
| `deploy/init-admin.sh` | 首次设置管理员密码并推进种子数据序列 |

## 服务器与域名

以 Ubuntu 24.04 LTS 为例。小规模使用可从 2 核 4 GB、40 GB 磁盘起步；并发和行情任务增加时需扩容。在服务器构建镜像时，4 GB 以上内存更合适。本配置不包含本地大模型，AI 模型在网站设置中配置为外部接口；Ollama 如需使用应另行部署。

服务器需要固定公网 IP、一个可管理 DNS 的域名，以及到镜像仓库、Maven、npm、PyPI、证书服务、SMTP、行情和 AI 接口的出站网络。若访问这些来源失败，应先解决服务器网络或采用可信镜像源，不要关闭 TLS 验证。

中国内地节点通过域名提供公网网站时，需要按接入商要求完成 ICP 备案，详见[阿里云备案说明](https://help.aliyun.com/zh/icp-filing/basic-icp-service/support/for-the-record-process-faq)。部署地区按用户位置、行情接口连通性和接入要求选择。

DNS 添加 A 记录，例如 `metrix.example.com` 指向服务器公网 IPv4。仅在服务器确实提供 IPv6 时添加 AAAA 记录，避免证书校验或访问落到错误地址。首次部署建议直接解析到服务器，先验证完成再接入 CDN。

云安全组开放：

| 端口 | 来源 | 用途 |
|---|---|---|
| TCP 22 或实际 SSH 端口 | 你的管理 IP | 服务器管理 |
| TCP 80 | 公网 | HTTP 跳转和证书校验 |
| TCP 443 | 公网 | HTTPS 网站 |

不开放 5432、6379、8080、8088。部署前检查 80/443 没有被已有 Nginx、Apache 或其他容器占用；已有站点应复用现有反向代理，或单独准备服务器。

```bash
sudo ss -lntp
```

## 安装 Docker

已安装 Docker Engine 和 Compose 时，直接检查版本并跳过安装。新 Ubuntu 服务器可以按 [Docker 官方安装说明](https://docs.docker.com/engine/install/ubuntu/)执行：

```bash
sudo apt update
sudo apt install -y ca-certificates curl git openssl dnsutils
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc

sudo tee /etc/apt/sources.list.d/docker.sources > /dev/null <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: $(. /etc/os-release && echo "$UBUNTU_CODENAME")
Components: stable
Architectures: $(dpkg --print-architecture)
Signed-By: /etc/apt/keyrings/docker.asc
EOF

sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker version
sudo docker compose version
```

后续命令需要有 Docker 权限；未配置部署账号权限时，在 `docker` 前加 `sudo`。Docker 用户组具有管理宿主机的权限，只应加入可信管理账号。主机防火墙如使用 UFW，应先允许实际 SSH 管理端口，再允许 80/443，避免断开 SSH。Docker 发布端口的防护还应依赖云安全组，详见[端口发布说明](https://docs.docker.com/engine/network/port-publishing/)。

## 上传项目与配置环境变量

在部署账号有写权限的目录克隆项目，私有仓库使用你的 SSH 密钥或现有 Git 凭据。仓库当前主分支为 `master`；也可以检出你准备发布的实际分支或标签。

```bash
git clone https://github.com/libin-tech/Metrix.git
cd Metrix
cp .env.example .env
chmod 600 .env
```

`.env` 是 Docker 部署配置，`backend/.env` 是本地 Java 开发配置。容器通过 Compose 注入变量，不读取宿主机的 `backend/.env`，不要把本地数据库地址复制到容器里。

分别运行下面命令生成三个不同的密码，填写 `POSTGRES_PASSWORD`、`REDIS_PASSWORD` 和 `ADMIN_PASSWORD`。示例模板没有默认密码，缺少密码时 Compose 会拒绝启动。

```bash
openssl rand -hex 24
```

编辑 `.env`，例如：

```dotenv
POSTGRES_PASSWORD='填写独立的随机数据库密码'
REDIS_PASSWORD='填写独立的随机Redis密码'
ADMIN_PASSWORD='填写独立的管理员登录密码'
FRONTEND_PORT=8088
METRIX_IMAGE_TAG=local
METRIX_DOMAIN=metrix.example.com
ACME_EMAIL=you@example.com
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=no-reply@example.com
MAIL_PASSWORD='填写邮件服务商的SMTP授权码'
AUTH_MAIL_FROM=no-reply@example.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_SSL_ENABLE=false
AUTH_CAPTCHA_ENABLED=true
```

将示例域名和邮件地址换成自己的。`METRIX_DOMAIN` 只填单个域名，不加 `https://`、路径或端口。密码包含 `$`、`#` 或空格时用单引号包裹；单引号的转义规则见 [Compose 环境变量文档](https://docs.docker.com/compose/how-tos/environment-variables/variable-interpolation/)。不要提交 `.env`，也不要分享 `docker compose config` 的完整输出，它会包含解析后的密码；使用 `config --quiet` 检查。

邮箱注册和密码重置需要 SMTP。587 通常使用 STARTTLS；465 通常设置 `MAIL_SMTP_STARTTLS_ENABLE=false` 和 `MAIL_SMTP_SSL_ENABLE=true`，以服务商要求为准。发件地址必须被邮件服务商允许，按服务商要求配置 SPF、DKIM 等 DNS 记录。暂不填写 SMTP 可先验证管理员登录，普通用户无法完成验证码注册或找回密码。

`ADMIN_PASSWORD` 仅在首次创建数据库时生效，初始化程序会覆盖种子脚本中的默认密码。用户名为 `admin`。它沿用应用当前的 MD5 密码校验方式；不要复用其他系统的密码。

`APP_MEMORY_LIMIT` 默认限制后端容器为 2 GB，包含 Java 和 Python 子进程；JVM 最大堆约为容器限额的 70%。高并发行情任务需调整限额和堆占比，并为数据库、Redis、操作系统留出内存。

## 首次启动与本机验证

基础服务先在服务器本机启动，数据库初始化完成后后端才会启动，后端健康后前端才会启动：

```bash
docker compose config --quiet
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
docker compose logs --tail=100 app
curl --fail http://127.0.0.1:8088/api/auth/verification-config
curl --fail http://127.0.0.1:8088/api/auth/captcha
```

如果修改了 `FRONTEND_PORT`，相应替换 curl 的端口。`up --wait` 的等待时间从容器启动开始，镜像构建和首次下载可能更久。检查服务全部健康；验证码接口应返回验证码 ID 和图片数据。若失败，按后文排查，不应通过关闭验证码来绕过 Redis 或字体问题。

第一次创建空的 PostgreSQL 数据卷时，依次执行：

1. `backend/.doc/db/ddl-init.sql`：建表。
2. `backend/.doc/db/dml-init.sql`：角色和管理员种子数据。
3. `backend/.doc/db/system_menu.sql`：菜单数据。
4. `backend/.doc/db/system_api.sql`：接口权限数据。
5. `deploy/init-admin.sh`：设置管理员密码并推进序列。

这些脚本只在空数据卷执行一次。正常重建容器不会清空数据，也不会再次导入。不要在 Compose 数据库上重复执行[部署指南](./deployment.md#本地开发)中的本地开发初始化命令。

可用 SSH 隧道从自己的电脑检查页面：

```bash
ssh -L 8088:127.0.0.1:8088 部署账号@服务器公网IP
```

然后在自己的浏览器打开 `http://127.0.0.1:8088`，使用 `admin` 和 `.env` 中的管理员密码登录。

## 启用公网 HTTPS

先检查 DNS 已指向服务器：

```bash
dig +short A metrix.example.com
dig +short AAAA metrix.example.com
```

启动公网入口：

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --build --wait --wait-timeout 300
docker compose -f docker-compose.yml -f docker-compose.public.yml logs --tail=100 caddy
curl -I http://metrix.example.com
curl --fail https://metrix.example.com/api/auth/verification-config
```

HTTP 应跳转至 HTTPS，HTTPS 应返回有效响应。Caddy 容器启动完成不代表证书已签发，初次签发需要等待日志中的成功信息或实际 HTTPS 请求成功。它自动申请、续期证书；DNS、80/443 可达、正确系统时间和持久化的 `/data` 都是正常运行的条件，见 [Caddy 自动 HTTPS 文档](https://caddyserver.com/docs/automatic-https)。本配置将证书存入 `caddy_data`，不要随意删除该卷。

从另一台设备或手机移动网络访问 `https://metrix.example.com`，验证以下流程：

- 管理员登录、验证码刷新、页面刷新和深层路由。
- 邮箱注册、验证码收信、普通用户登录及密码重置。
- 普通用户无法进入管理页面。
- 配置外部 AI、新闻和行情接口后，验证行情同步和分析任务。
- 问一问内容逐步出现，而不是等待结束一次性返回。
- 浏览器开发者工具中 `/api/ws/market-dashboard` 握手状态为 101，且收到实时消息。

生产前端统一使用相对路径 `/api`；不需要修改 Vite 的开发代理或 `allowedHosts`。WebSocket 地址随页面协议选择 `ws` 或 `wss`。Nginx 已配置 Upgrade 转发和 SSE 关闭缓冲，Caddy 使用立即刷新，见 [Nginx WebSocket 说明](https://nginx.org/en/docs/http/websocket.html)和 [Caddy 代理说明](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)。两级代理之后再加 CDN 或负载均衡器时，也要检查其流式响应和连接超时。

部署公网服务后，管理命令统一带两个 `-f` 参数，确保操作包含 Caddy：

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml ps
docker compose -f docker-compose.yml -f docker-compose.public.yml logs -f app
docker compose -f docker-compose.yml -f docker-compose.public.yml restart caddy
```

修改 `.env` 后使用 `up -d` 重建相关容器，`restart` 不会重新注入环境变量。

## 持久化与备份

| 数据卷 | 内容 |
|---|---|
| `postgres_data` | 用户、权限、配置、持仓和分析记录 |
| `redis_data` | Redis AOF、会话和验证码缓存 |
| `app_logs` | 应用日志文件 |
| `caddy_data` | TLS 私钥、证书及 ACME 账号 |
| `caddy_config` | Caddy 运行配置数据 |

Compose 项目名固定为 `metrix`，卷名通常带 `metrix_` 前缀。改用 `-p` 会使用另一组卷，表现为新数据库；迁移旧部署时必须确认实际项目名与数据卷。`docker compose down` 保留命名卷，**`down -v` 会删除数据卷**，生产环境不要执行。容器日志设置每服务 3 个 10 MB 文件；应用文件日志另存于 `app_logs`，仍需关注磁盘占用。

手动备份数据库：

```bash
mkdir -p backups
chmod 700 backups
umask 077
backupFile="backups/metrix-$(date +%Y%m%d-%H%M%S).dump"
docker compose exec -T postgres pg_dump -U postgres -d stock_analysis -Fc > "$backupFile"
test -s "$backupFile"
docker compose exec -T postgres pg_restore --list < "$backupFile" > /dev/null
```

数据库备份含账号和 API 配置，应加密保存到另一台机器或对象存储。另行安全保存 `.env` 和证书卷，不能只保存在同一服务器上。安排定期备份和保留策略，并在隔离环境试恢复；列出备份目录不等于完成恢复验证。

恢复会覆盖目标数据库现有表和数据。先备份当前数据库，确认备份文件和目标环境，再暂停外部入口与业务容器：

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml stop caddy frontend app
docker compose exec -T postgres pg_restore -U postgres -d stock_analysis \
  --clean --if-exists --exit-on-error < backups/要恢复的备份.dump
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --wait --wait-timeout 300
```

恢复后的管理员密码来自备份中的用户记录，不会被 `ADMIN_PASSWORD` 重置。Redis 保留的旧登录会话应在跨环境恢复时失效，可在确认目标 Redis 专属本项目后清理 Redis 或换用空 Redis 数据卷。

## 升级与回滚

部署前完成数据库备份，并记录代码提交和当前镜像标签。为了保留回滚镜像，每次发布使用不同的 `METRIX_IMAGE_TAG`，例如提交短哈希或版本号；不要一直覆盖 `local`：

```bash
git rev-parse HEAD
# 更新代码并将 .env 的 METRIX_IMAGE_TAG 改为本次发布标签。
docker compose -f docker-compose.yml -f docker-compose.public.yml build
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --wait --wait-timeout 300
```

现有数据库不会自动执行增量迁移。涉及表结构的发布，先审核该版本提供的迁移文件并按版本顺序执行；仓库当前提供的是完整初始化脚本，不能作为已有数据库的升级脚本重复导入。

回滚时先检出前一版本的代码和部署配置，把 `.env` 的 `METRIX_IMAGE_TAG` 改回保留的旧标签，再运行：

```bash
docker compose -f docker-compose.yml -f docker-compose.public.yml up -d --no-build --wait --wait-timeout 300
```

旧镜像必须仍在服务器上，或已保存到私有镜像仓库。回滚应用不会回滚数据库；若发布包含不兼容的结构变更，需使用匹配的恢复方案，并评估恢复点之后的数据丢失。正常更新会短暂中断请求和 WebSocket，本方案为单机部署，不提供零停机保障。

修改数据库密码时，修改 `.env` 不会改变已有 PostgreSQL 账号密码。先通过 `docker compose exec postgres psql -U postgres -d stock_analysis` 中的 `\password postgres` 更新账号，再同步 `.env` 并用 `up -d` 重建服务。Redis 密码变更也必须同时重建 Redis 和后端。

## 已有数据库

初始化仅适用于全新空卷。已有 Metrix 数据应先备份，再确认 schema、账号、菜单、接口数据和序列已满足当前应用要求。如果需要迁移到新的 Compose 卷，先启动 PostgreSQL，恢复备份，再启动应用；初始化生成的管理员将被备份中的记录覆盖。

旧 Compose 可能使用不同项目名、容器名或密码。不要直接更换项目名或删除旧卷；用 `docker volume ls` 和旧部署的 `docker compose ps` 找到实际资源，确认新部署连接的是预期数据库。当前 SQL 的表所有者为 `postgres`，Compose 因此固定使用该数据库账号；已有外部数据库的账号与授权需按实际环境单独调整。

首次初始化中途失败可能留下部分建表的数据卷，重启不会自动续跑。查看 PostgreSQL 日志定位失败脚本；有数据时备份并人工修复。仅在明确这是可以丢弃的全新失败初始化环境时，才删除对应测试卷后重新初始化。

## 常见问题

| 现象 | 检查方式 |
|---|---|
| Compose 提示缺少密码 | 检查仓库根目录 `.env` 的三个密码；`backend/.env` 不替代它 |
| 构建找不到后端源码或 Python | 在仓库根目录执行 Compose；直接构建后端用 `docker build -f backend/Dockerfile .` |
| 下载镜像、Maven、npm、pip 超时 | 检查服务器出站 DNS 和对应仓库连通性 |
| PostgreSQL 不健康 | 查看 `docker compose logs --tail=100 postgres`，检查初始化 SQL、卷权限和磁盘 |
| Redis 不健康 | 查看 Redis 日志，确认密码已注入且 AOF 目录可写 |
| app 不健康 | 查看后端日志和内部 `/actuator/health`，检查数据库和 Redis；SMTP 不参与健康判断 |
| SMTP 收不到验证码 | 核对授权码、发件人、587/465 加密设置、云服务商出站限制和垃圾邮件目录 |
| 公网域名无法访问 | 核对 A/AAAA、云安全组、80/443 占用和 Caddy 日志 |
| 证书申请失败 | 核对公网 DNS 和验证端口，避免连续反复重建；不要删除证书卷来重试 |
| 网站返回 502 | 查看 app 健康状态、前端代理日志和 Caddy 日志 |
| SSE 不流式或 WebSocket 中断 | 核对 CDN 代理策略；无消息超过 600 秒的连接会被 Nginx 断开 |
| Python 行情接口报错 | 检查容器 Python 导入和第三方行情接口连通性；外部数据接口可能限流 |
| 重启后数据像丢失 | 检查 Compose 项目名、实际数据卷，是否误用了新的 `-p` |
| 改 ADMIN_PASSWORD 后登录密码没变 | 已有数据库不会重跑初始化，需按现有账号管理流程修改密码 |

内部检查命令：

```bash
docker compose exec app curl --fail http://127.0.0.1:8080/actuator/health
docker compose exec app python -c 'import akshare, baostock, requests, pandas; print("Python dependencies OK")'
docker compose exec frontend nginx -t
docker compose -f docker-compose.yml -f docker-compose.public.yml exec caddy \
  caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile
docker system df
```

正常健康检查包含 PostgreSQL 和 Redis，详情不向 HTTP 响应公开；Actuator 配置参考 [Spring Boot 说明](https://docs.spring.io/spring-boot/reference/actuator/monitoring.html)。健康检查失败不会自动重启一个仍在运行的容器，需查看日志解决根因；`restart: unless-stopped` 处理的是进程退出和 Docker 重启。
