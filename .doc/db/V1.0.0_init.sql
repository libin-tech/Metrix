-- ============================================================
-- 版本: V1
-- 描述: 初始化数据库表结构
-- 说明: 根据实体类逆向生成，包含表注释和字段注释
-- ============================================================

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id              BIGSERIAL           PRIMARY KEY,
    username        VARCHAR(50)         NOT NULL,
    password        VARCHAR(255)        NOT NULL,
    email           VARCHAR(100),
    role            VARCHAR(20)         NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN             NOT NULL DEFAULT TRUE,
    create_time     TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP WITHOUT TIME ZONE,
    version         INTEGER             NOT NULL DEFAULT 0,
    creator         VARCHAR(50),
    modifier        VARCHAR(50)
);

COMMENT ON TABLE users IS '用户表';
COMMENT ON COLUMN users.id IS '主键ID';
COMMENT ON COLUMN users.username IS '用户名';
COMMENT ON COLUMN users.password IS '密码（MD5加密）';
COMMENT ON COLUMN users.email IS '邮箱';
COMMENT ON COLUMN users.role IS '角色（ADMIN/USER）';
COMMENT ON COLUMN users.is_active IS '是否激活';
COMMENT ON COLUMN users.create_time IS '创建时间';
COMMENT ON COLUMN users.update_time IS '更新时间';
COMMENT ON COLUMN users.version IS '版本号（乐观锁）';
COMMENT ON COLUMN users.creator IS '创建人';
COMMENT ON COLUMN users.modifier IS '修改人';

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email ON users (email);

-- 插入默认管理员用户（密码：admin@2026，MD5加密后为：dffd3d7872472b8fce1750da3f1e3cbd）
INSERT INTO users (username, password, email, role, is_active, create_time, update_time, version, creator, modifier)
VALUES ('admin', 'dffd3d7872472b8fce1750da3f1e3cbd', 'admin@example.com', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0, 'SYSTEM', 'SYSTEM')
ON CONFLICT (username) DO NOTHING;

-- 股票分析记录表
CREATE TABLE IF NOT EXISTS stock_analysis_record (
    id                  BIGSERIAL           PRIMARY KEY,
    stock_code          VARCHAR(20)         NOT NULL,
    stock_name          VARCHAR(100)        NOT NULL,
    analysis_type       VARCHAR(50)         NOT NULL,
    analysis_result     TEXT,
    confidence_score    NUMERIC(5, 4),
    market_data         TEXT,
    news_summary        TEXT,
    create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITHOUT TIME ZONE,
    version             INTEGER             NOT NULL DEFAULT 0,
    creator             VARCHAR(50),
    modifier            VARCHAR(50)
);

COMMENT ON TABLE stock_analysis_record IS '股票分析记录表';
COMMENT ON COLUMN stock_analysis_record.id IS '主键ID';
COMMENT ON COLUMN stock_analysis_record.stock_code IS '股票代码';
COMMENT ON COLUMN stock_analysis_record.stock_name IS '股票名称';
COMMENT ON COLUMN stock_analysis_record.analysis_type IS '分析类型';
COMMENT ON COLUMN stock_analysis_record.analysis_result IS '分析结果';
COMMENT ON COLUMN stock_analysis_record.confidence_score IS '置信度分数';
COMMENT ON COLUMN stock_analysis_record.market_data IS '市场数据（JSON格式）';
COMMENT ON COLUMN stock_analysis_record.news_summary IS '新闻摘要（JSON格式）';
COMMENT ON COLUMN stock_analysis_record.create_time IS '创建时间';
COMMENT ON COLUMN stock_analysis_record.update_time IS '更新时间';
COMMENT ON COLUMN stock_analysis_record.version IS '版本号（乐观锁）';
COMMENT ON COLUMN stock_analysis_record.creator IS '创建人';
COMMENT ON COLUMN stock_analysis_record.modifier IS '修改人';

CREATE INDEX IF NOT EXISTS idx_analysis_stock_code ON stock_analysis_record (stock_code);
CREATE INDEX IF NOT EXISTS idx_analysis_created_at ON stock_analysis_record (create_time);

-- 市场数据配置表
CREATE TABLE IF NOT EXISTS market_data_config (
    id                  BIGSERIAL           PRIMARY KEY,
    source_name         VARCHAR(50)         NOT NULL,
    api_url             VARCHAR(500)        NOT NULL,
    api_key             VARCHAR(500),
    data_type           VARCHAR(50)         NOT NULL,
    request_interval    INTEGER             NOT NULL DEFAULT 60,
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITHOUT TIME ZONE,
    version             INTEGER             NOT NULL DEFAULT 0,
    creator             VARCHAR(50),
    modifier            VARCHAR(50)
);

COMMENT ON TABLE market_data_config IS '市场数据配置表';
COMMENT ON COLUMN market_data_config.id IS '主键ID';
COMMENT ON COLUMN market_data_config.source_name IS '源名称（TICKFLOW）';
COMMENT ON COLUMN market_data_config.api_url IS 'API URL';
COMMENT ON COLUMN market_data_config.api_key IS 'API密钥';
COMMENT ON COLUMN market_data_config.data_type IS '数据类型';
COMMENT ON COLUMN market_data_config.request_interval IS '请求间隔（秒）';
COMMENT ON COLUMN market_data_config.is_active IS '是否激活';
COMMENT ON COLUMN market_data_config.create_time IS '创建时间';
COMMENT ON COLUMN market_data_config.update_time IS '更新时间';
COMMENT ON COLUMN market_data_config.version IS '版本号（乐观锁）';
COMMENT ON COLUMN market_data_config.creator IS '创建人';
COMMENT ON COLUMN market_data_config.modifier IS '修改人';

CREATE INDEX IF NOT EXISTS idx_market_data_source ON market_data_config (source_name);

-- 新闻源配置表
CREATE TABLE IF NOT EXISTS news_source_config (
    id                  BIGSERIAL           PRIMARY KEY,
    source_name         VARCHAR(50)         NOT NULL,
    api_url             VARCHAR(500)        NOT NULL,
    api_key             VARCHAR(500),
    request_interval    INTEGER             NOT NULL DEFAULT 60,
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITHOUT TIME ZONE,
    version             INTEGER             NOT NULL DEFAULT 0,
    creator             VARCHAR(50),
    modifier            VARCHAR(50)
);

COMMENT ON TABLE news_source_config IS '新闻源配置表';
COMMENT ON COLUMN news_source_config.id IS '主键ID';
COMMENT ON COLUMN news_source_config.source_name IS '源名称（BOCHA）';
COMMENT ON COLUMN news_source_config.api_url IS 'API URL';
COMMENT ON COLUMN news_source_config.api_key IS 'API密钥';
COMMENT ON COLUMN news_source_config.request_interval IS '请求间隔（秒）';
COMMENT ON COLUMN news_source_config.is_active IS '是否激活';
COMMENT ON COLUMN news_source_config.create_time IS '创建时间';
COMMENT ON COLUMN news_source_config.update_time IS '更新时间';
COMMENT ON COLUMN news_source_config.version IS '版本号（乐观锁）';
COMMENT ON COLUMN news_source_config.creator IS '创建人';
COMMENT ON COLUMN news_source_config.modifier IS '修改人';

CREATE INDEX IF NOT EXISTS idx_news_source ON news_source_config (source_name);

-- 通知配置表
CREATE TABLE IF NOT EXISTS notification_config (
    id                  BIGSERIAL           PRIMARY KEY,
    channel_type        VARCHAR(50)         NOT NULL,
    webhook_url         VARCHAR(500)        NOT NULL,
    secret              VARCHAR(500),
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITHOUT TIME ZONE,
    version             INTEGER             NOT NULL DEFAULT 0,
    creator             VARCHAR(50),
    modifier            VARCHAR(50)
);

COMMENT ON TABLE notification_config IS '通知配置表';
COMMENT ON COLUMN notification_config.id IS '主键ID';
COMMENT ON COLUMN notification_config.channel_type IS '渠道类型（FEISHU）';
COMMENT ON COLUMN notification_config.webhook_url IS 'WebHook URL';
COMMENT ON COLUMN notification_config.secret IS '密钥';
COMMENT ON COLUMN notification_config.is_active IS '是否激活';
COMMENT ON COLUMN notification_config.create_time IS '创建时间';
COMMENT ON COLUMN notification_config.update_time IS '更新时间';
COMMENT ON COLUMN notification_config.version IS '版本号（乐观锁）';
COMMENT ON COLUMN notification_config.creator IS '创建人';
COMMENT ON COLUMN notification_config.modifier IS '修改人';

CREATE INDEX IF NOT EXISTS idx_notification_channel ON notification_config (channel_type);

-- AI模型配置表
CREATE TABLE IF NOT EXISTS ai_model_config (
    id                  BIGSERIAL           PRIMARY KEY,
    model_type          VARCHAR(50)         NOT NULL,
    model_name          VARCHAR(100)        NOT NULL,
    api_base_url        VARCHAR(500)        NOT NULL,
    api_key             VARCHAR(500),
    temperature         DOUBLE PRECISION    DEFAULT 0.7,
    is_active           BOOLEAN             NOT NULL DEFAULT TRUE,
    create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
    update_time         TIMESTAMP WITHOUT TIME ZONE,
    version             INTEGER             NOT NULL DEFAULT 0,
    creator             VARCHAR(50),
    modifier            VARCHAR(50)
);

COMMENT ON TABLE ai_model_config IS 'AI模型配置表';
COMMENT ON COLUMN ai_model_config.id IS '主键ID';
COMMENT ON COLUMN ai_model_config.model_type IS '模型类型（OPENAI/OLLAMA）';
COMMENT ON COLUMN ai_model_config.model_name IS '模型名称';
COMMENT ON COLUMN ai_model_config.api_base_url IS 'API基础URL';
COMMENT ON COLUMN ai_model_config.api_key IS 'API密钥';
COMMENT ON COLUMN ai_model_config.temperature IS '温度参数';
COMMENT ON COLUMN ai_model_config.is_active IS '是否激活';
COMMENT ON COLUMN ai_model_config.create_time IS '创建时间';
COMMENT ON COLUMN ai_model_config.update_time IS '更新时间';
COMMENT ON COLUMN ai_model_config.version IS '版本号（乐观锁）';
COMMENT ON COLUMN ai_model_config.creator IS '创建人';
COMMENT ON COLUMN ai_model_config.modifier IS '修改人';

CREATE INDEX IF NOT EXISTS idx_ai_model_type ON ai_model_config (model_type);


-- 股票基本信息表
CREATE TABLE IF NOT EXISTS stock_basic (
                                           id                  BIGSERIAL           PRIMARY KEY,
                                           ts_code             VARCHAR(20)         NOT NULL,
                                           symbol              VARCHAR(6)          NOT NULL,
                                           name                VARCHAR(100)        NOT NULL,
                                           area                VARCHAR(50),
                                           industry            VARCHAR(100),
                                           cnspell             VARCHAR(20),
                                           market              VARCHAR(20),
                                           list_date           DATE,
                                           act_name            VARCHAR(200),
                                           act_ent_type        VARCHAR(50),
                                           create_time         TIMESTAMP WITHOUT TIME ZONE    NOT NULL DEFAULT NOW(),
                                           update_time         TIMESTAMP WITHOUT TIME ZONE,
                                           version             INTEGER             NOT NULL DEFAULT 0,
                                           creator             VARCHAR(50),
                                           modifier            VARCHAR(50)
);

COMMENT ON TABLE stock_basic IS 'A股股票基本信息表';
COMMENT ON COLUMN stock_basic.id IS '主键ID';
COMMENT ON COLUMN stock_basic.ts_code IS 'TS股票代码（唯一标识）';
COMMENT ON COLUMN stock_basic.symbol IS '股票代码';
COMMENT ON COLUMN stock_basic.name IS '股票名称';
COMMENT ON COLUMN stock_basic.area IS '地域';
COMMENT ON COLUMN stock_basic.industry IS '所属行业';
COMMENT ON COLUMN stock_basic.cnspell IS '拼音缩写';
COMMENT ON COLUMN stock_basic.market IS '市场类型（主板/创业板/科创板）';
COMMENT ON COLUMN stock_basic.list_date IS '上市日期';
COMMENT ON COLUMN stock_basic.act_name IS '实际控制人名称';
COMMENT ON COLUMN stock_basic.act_ent_type IS '企业性质';
COMMENT ON COLUMN stock_basic.create_time IS '创建时间';
COMMENT ON COLUMN stock_basic.update_time IS '更新时间';
COMMENT ON COLUMN stock_basic.version IS '版本号（乐观锁）';
COMMENT ON COLUMN stock_basic.creator IS '创建人';
COMMENT ON COLUMN stock_basic.modifier IS '修改人';

CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_basic_ts_code ON stock_basic (ts_code);
CREATE UNIQUE INDEX IF NOT EXISTS idx_stock_basic_symbol ON stock_basic (symbol);
