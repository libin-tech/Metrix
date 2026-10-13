#!/usr/bin/env bash
set -euo pipefail

# PostgreSQL 官方镜像仅在空数据卷初始化时执行本脚本。
: "${ADMIN_PASSWORD:?ADMIN_PASSWORD must be set}"
psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=ON_ERROR_STOP=1 --set=adminPassword="$ADMIN_PASSWORD" <<'SQL'
BEGIN;
UPDATE users SET password = md5(:'adminPassword'), update_time = now()
WHERE username = 'admin';

-- 种子数据使用显式 ID，推进序列，避免后续新增数据与种子 ID 冲突。
SELECT setval(pg_get_serial_sequence('users', 'id'), (SELECT max(id) FROM users));
SELECT setval(pg_get_serial_sequence('system_role', 'id'), (SELECT max(id) FROM system_role));
SELECT setval(pg_get_serial_sequence('system_user_role', 'id'), (SELECT max(id) FROM system_user_role));
SELECT setval(pg_get_serial_sequence('system_menu', 'id'), (SELECT max(id) FROM system_menu));
SELECT setval(pg_get_serial_sequence('system_api', 'id'), (SELECT max(id) FROM system_api));
COMMIT;
SQL
