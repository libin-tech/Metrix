-- ----------------------------
-- Chat2DB export data , export time: 2026-05-14 14:57:58
-- ----------------------------
DROP SEQUENCE IF EXISTS "users_id_seq";
CREATE SEQUENCE "users_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "stock_analysis_record_id_seq";
CREATE SEQUENCE "stock_analysis_record_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "market_data_config_id_seq";
CREATE SEQUENCE "market_data_config_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "news_source_config_id_seq";
CREATE SEQUENCE "news_source_config_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "notification_config_id_seq";
CREATE SEQUENCE "notification_config_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "ai_model_config_id_seq";
CREATE SEQUENCE "ai_model_config_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;

DROP SEQUENCE IF EXISTS "stock_basic_id_seq";
CREATE SEQUENCE "stock_basic_id_seq"
 START WITH 1
 INCREMENT BY 1
 MAXVALUE 9223372036854775807
 MINVALUE 1
 CACHE 1
NO CYCLE
;


DROP TABLE IF EXISTS "users";
create table "users"
(
	id  	bigint default nextval('users_id_seq'::regclass) not null,
	username  	varchar(50) not null,
	password  	varchar(255) not null,
	email  	varchar(100),
	role  	varchar(20) default 'USER'::character varying not null,
	is_active  	boolean default true not null,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	 constraint users_pkey primary key (id)

) tablespace pg_default;
CREATE UNIQUE INDEX idx_users_username ON public.users USING btree (username);
CREATE UNIQUE INDEX idx_users_email ON public.users USING btree (email);

comment on table users is '用户表';
comment on column users.id is '主键ID';
comment on column users.username is '用户名';
comment on column users.password is '密码（MD5加密）';
comment on column users.email is '邮箱';
comment on column users.role is '角色（ADMIN/USER）';
comment on column users.is_active is '是否激活';
comment on column users.create_time is '创建时间';
comment on column users.update_time is '更新时间';
comment on column users.version is '版本号（乐观锁）';
comment on column users.creator is '创建人';
comment on column users.modifier is '修改人';

alter table "users" owner to postgres;



DROP TABLE IF EXISTS "market_data_config";
create table "market_data_config"
(
	id  	bigint default nextval('market_data_config_id_seq'::regclass) not null,
	source_name  	varchar(50) not null,
	api_url  	varchar(500) not null,
	api_key  	varchar(500),
	data_type  	varchar(50) not null,
	request_interval  	integer default 60 not null,
	is_active  	boolean default true not null,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	timeout  	integer default 60 not null,
	remark  	varchar(100),
	 constraint market_data_config_pkey primary key (id)

) tablespace pg_default;
CREATE INDEX idx_market_data_source ON public.market_data_config USING btree (source_name);

comment on table market_data_config is '市场数据配置表';
comment on column market_data_config.id is '主键ID';
comment on column market_data_config.source_name is '源名称（TICKFLOW）';
comment on column market_data_config.api_url is 'API URL';
comment on column market_data_config.api_key is 'API密钥';
comment on column market_data_config.data_type is '数据类型';
comment on column market_data_config.request_interval is '请求间隔（秒）';
comment on column market_data_config.is_active is '是否激活';
comment on column market_data_config.create_time is '创建时间';
comment on column market_data_config.update_time is '更新时间';
comment on column market_data_config.version is '版本号（乐观锁）';
comment on column market_data_config.creator is '创建人';
comment on column market_data_config.modifier is '修改人';
comment on column market_data_config.timeout is '请求超时时间（秒），默认60秒';
comment on column market_data_config.remark is '备注，限制100字以内';

alter table "market_data_config" owner to postgres;



DROP TABLE IF EXISTS "notification_config";
create table "notification_config"
(
	id  	bigint default nextval('notification_config_id_seq'::regclass) not null,
	channel_type  	varchar(50) not null,
	webhook_url  	varchar(500) not null,
	secret  	varchar(500),
	is_active  	boolean default true not null,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	 constraint notification_config_pkey primary key (id)

) tablespace pg_default;
CREATE INDEX idx_notification_channel ON public.notification_config USING btree (channel_type);

comment on table notification_config is '通知配置表';
comment on column notification_config.id is '主键ID';
comment on column notification_config.channel_type is '渠道类型（FEISHU）';
comment on column notification_config.webhook_url is 'WebHook URL';
comment on column notification_config.secret is '密钥';
comment on column notification_config.is_active is '是否激活';
comment on column notification_config.create_time is '创建时间';
comment on column notification_config.update_time is '更新时间';
comment on column notification_config.version is '版本号（乐观锁）';
comment on column notification_config.creator is '创建人';
comment on column notification_config.modifier is '修改人';

alter table "notification_config" owner to postgres;



DROP TABLE IF EXISTS "stock_analysis_record";
create table "stock_analysis_record"
(
	id  	bigint default nextval('stock_analysis_record_id_seq'::regclass) not null,
	stock_code  	varchar(20) not null,
	stock_name  	varchar(100) not null,
	analysis_type  	varchar(50) not null,
	analysis_result  	text,
	market_data  	text,
	news_summary  	text,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	status  	varchar(20) default 'COMPLETED'::character varying not null,
	analysis_overview  	text,
	depth_data  	text,
	klines_data  	text,
	 constraint stock_analysis_record_pkey primary key (id)

) tablespace pg_default;
CREATE INDEX idx_analysis_stock_code ON public.stock_analysis_record USING btree (stock_code);
CREATE INDEX idx_analysis_created_at ON public.stock_analysis_record USING btree (create_time);
CREATE INDEX idx_stock_analysis_record_status ON public.stock_analysis_record USING btree (status);

comment on table stock_analysis_record is '股票分析记录表';
comment on column stock_analysis_record.id is '主键ID';
comment on column stock_analysis_record.stock_code is '股票代码';
comment on column stock_analysis_record.stock_name is '股票名称';
comment on column stock_analysis_record.analysis_type is '分析类型';
comment on column stock_analysis_record.analysis_result is '分析结果';
comment on column stock_analysis_record.market_data is '实时行情（JSON格式）';
comment on column stock_analysis_record.news_summary is '新闻摘要（JSON格式）';
comment on column stock_analysis_record.create_time is '创建时间';
comment on column stock_analysis_record.update_time is '更新时间';
comment on column stock_analysis_record.version is '版本号（乐观锁）';
comment on column stock_analysis_record.creator is '创建人';
comment on column stock_analysis_record.modifier is '修改人';
comment on column stock_analysis_record.status is '分析状态：ANALYZING-分析中，COMPLETED-分析完成，FAILED-分析失败';
comment on column stock_analysis_record.analysis_overview is '分析概览（JSON格式）';
comment on column stock_analysis_record.depth_data is '深度行情（JSON格式）';
comment on column stock_analysis_record.klines_data is '日K数据（JSON格式）';

alter table "stock_analysis_record" owner to postgres;



DROP TABLE IF EXISTS "ai_model_config";
create table "ai_model_config"
(
	id  	bigint default nextval('ai_model_config_id_seq'::regclass) not null,
	model_type  	varchar(50) not null,
	model_name  	varchar(100) not null,
	api_base_url  	varchar(500),
	api_key  	varchar(500),
	temperature  	double precision default 0.7,
	is_active  	boolean default true not null,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	timeout  	integer default 120 not null,
	 constraint ai_model_config_pkey primary key (id)

) tablespace pg_default;
CREATE INDEX idx_ai_model_type ON public.ai_model_config USING btree (model_type);

comment on table ai_model_config is 'AI模型配置表';
comment on column ai_model_config.id is '主键ID';
comment on column ai_model_config.model_type is '模型类型（OPENAI/OLLAMA/GEMINI';
comment on column ai_model_config.model_name is '模型名称';
comment on column ai_model_config.api_base_url is 'API基础URL';
comment on column ai_model_config.api_key is 'API密钥';
comment on column ai_model_config.temperature is '温度参数';
comment on column ai_model_config.is_active is '是否激活';
comment on column ai_model_config.create_time is '创建时间';
comment on column ai_model_config.update_time is '更新时间';
comment on column ai_model_config.version is '版本号（乐观锁）';
comment on column ai_model_config.creator is '创建人';
comment on column ai_model_config.modifier is '修改人';
comment on column ai_model_config.timeout is '超时时间（秒），默认120秒';

alter table "ai_model_config" owner to postgres;



DROP TABLE IF EXISTS "news_source_config";
create table "news_source_config"
(
	id  	bigint default nextval('news_source_config_id_seq'::regclass) not null,
	source_name  	varchar(50) not null,
	api_url  	varchar(500) not null,
	api_key  	varchar(500),
	request_interval  	integer default 60 not null,
	is_active  	boolean default true not null,
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	timeout  	integer default 60 not null,
	remark  	varchar(100),
	 constraint news_source_config_pkey primary key (id)

) tablespace pg_default;
CREATE INDEX idx_news_source ON public.news_source_config USING btree (source_name);

comment on table news_source_config is '新闻源配置表';
comment on column news_source_config.id is '主键ID';
comment on column news_source_config.source_name is '源名称（BOCHA）';
comment on column news_source_config.api_url is 'API URL';
comment on column news_source_config.api_key is 'API密钥';
comment on column news_source_config.request_interval is '请求间隔（秒）';
comment on column news_source_config.is_active is '是否激活';
comment on column news_source_config.create_time is '创建时间';
comment on column news_source_config.update_time is '更新时间';
comment on column news_source_config.version is '版本号（乐观锁）';
comment on column news_source_config.creator is '创建人';
comment on column news_source_config.modifier is '修改人';
comment on column news_source_config.timeout is '请求超时时间（秒），默认60秒';
comment on column news_source_config.remark is '备注，限制100字以内';

alter table "news_source_config" owner to postgres;



DROP TABLE IF EXISTS "stock_basic";
create table "stock_basic"
(
	id  	bigint default nextval('stock_basic_id_seq'::regclass) not null,
	ts_code  	varchar(20) not null,
	symbol  	varchar(6) not null,
	name  	varchar(100) not null,
	area  	varchar(50),
	industry  	varchar(100),
	cnspell  	varchar(20),
	market  	varchar(20),
	list_date  	date,
	act_name  	varchar(200),
	act_ent_type  	varchar(50),
	create_time  	timestamp default now() not null,
	update_time  	timestamp,
	version  	integer default 0 not null,
	creator  	varchar(50),
	modifier  	varchar(50),
	 constraint stock_basic_pkey primary key (id)

) tablespace pg_default;
CREATE UNIQUE INDEX idx_stock_basic_ts_code ON public.stock_basic USING btree (ts_code);
CREATE UNIQUE INDEX idx_stock_basic_symbol ON public.stock_basic USING btree (symbol);

comment on table stock_basic is 'A股股票基本信息表';
comment on column stock_basic.id is '主键ID';
comment on column stock_basic.ts_code is 'TS股票代码（唯一标识）';
comment on column stock_basic.symbol is '股票代码';
comment on column stock_basic.name is '股票名称';
comment on column stock_basic.area is '地域';
comment on column stock_basic.industry is '所属行业';
comment on column stock_basic.cnspell is '拼音缩写';
comment on column stock_basic.market is '市场类型（主板/创业板/科创板）';
comment on column stock_basic.list_date is '上市日期';
comment on column stock_basic.act_name is '实际控制人名称';
comment on column stock_basic.act_ent_type is '企业性质';
comment on column stock_basic.create_time is '创建时间';
comment on column stock_basic.update_time is '更新时间';
comment on column stock_basic.version is '版本号（乐观锁）';
comment on column stock_basic.creator is '创建人';
comment on column stock_basic.modifier is '修改人';

alter table "stock_basic" owner to postgres;


-- ----------------------------
-- 我的持仓 - 券商账户表
-- ----------------------------
DROP SEQUENCE IF EXISTS "broker_account_id_seq";
CREATE SEQUENCE "broker_account_id_seq"
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807
    MINVALUE 1
    CACHE 1
    NO CYCLE;

DROP TABLE IF EXISTS "broker_account";
CREATE TABLE "broker_account"
(
    id             BIGINT DEFAULT NEXTVAL('broker_account_id_seq'::REGCLASS) NOT NULL,
    broker_name    VARCHAR(10)  NOT NULL,
    account_number VARCHAR(30),
    remark         VARCHAR(50),
    create_time    TIMESTAMP    DEFAULT NOW()                               NOT NULL,
    update_time    TIMESTAMP,
    version        INTEGER      DEFAULT 0                                   NOT NULL,
    creator        VARCHAR(50),
    modifier       VARCHAR(50),
    CONSTRAINT broker_account_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

COMMENT ON TABLE broker_account IS '券商账户';
COMMENT ON COLUMN broker_account.id IS '主键ID';
COMMENT ON COLUMN broker_account.broker_name IS '券商名称';
COMMENT ON COLUMN broker_account.account_number IS '券商账号';
COMMENT ON COLUMN broker_account.remark IS '备注';
COMMENT ON COLUMN broker_account.create_time IS '创建时间';
COMMENT ON COLUMN broker_account.update_time IS '更新时间';
COMMENT ON COLUMN broker_account.version IS '版本号（乐观锁）';
COMMENT ON COLUMN broker_account.creator IS '创建人';
COMMENT ON COLUMN broker_account.modifier IS '修改人';

ALTER TABLE "broker_account" OWNER TO postgres;

-- ----------------------------
-- 我的持仓 - 持仓表
-- ----------------------------
DROP SEQUENCE IF EXISTS "portfolio_holding_id_seq";
CREATE SEQUENCE "portfolio_holding_id_seq"
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807
    MINVALUE 1
    CACHE 1
    NO CYCLE;

DROP TABLE IF EXISTS "portfolio_holding";
CREATE TABLE "portfolio_holding"
(
    id              BIGINT DEFAULT NEXTVAL('portfolio_holding_id_seq'::REGCLASS) NOT NULL,
    account_id      BIGINT       NOT NULL,
    stock_code      VARCHAR(50)  NOT NULL,
    stock_name      VARCHAR(100) NOT NULL,
    cost            DECIMAL(20, 4),
    quantity        DECIMAL(20, 2),
    create_time     TIMESTAMP    DEFAULT NOW()                                   NOT NULL,
    update_time     TIMESTAMP,
    version         INTEGER      DEFAULT 0                                       NOT NULL,
    creator         VARCHAR(50),
    modifier        VARCHAR(50),
    CONSTRAINT portfolio_holding_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

CREATE INDEX idx_holding_account_id ON public.portfolio_holding USING btree (account_id);
CREATE INDEX idx_holding_stock_code ON public.portfolio_holding USING btree (stock_code);

COMMENT ON TABLE portfolio_holding IS '我的持仓表';
COMMENT ON COLUMN portfolio_holding.id IS '主键ID';
COMMENT ON COLUMN portfolio_holding.account_id IS '券商账户ID';
COMMENT ON COLUMN portfolio_holding.stock_code IS '标的代码';
COMMENT ON COLUMN portfolio_holding.stock_name IS '标的名称';
COMMENT ON COLUMN portfolio_holding.cost IS '成本';
COMMENT ON COLUMN portfolio_holding.quantity IS '数量';
COMMENT ON COLUMN portfolio_holding.create_time IS '创建时间';
COMMENT ON COLUMN portfolio_holding.update_time IS '更新时间';
COMMENT ON COLUMN portfolio_holding.version IS '版本号（乐观锁）';
COMMENT ON COLUMN portfolio_holding.creator IS '创建人';
COMMENT ON COLUMN portfolio_holding.modifier IS '修改人';

ALTER TABLE "portfolio_holding" OWNER TO postgres;


-- 大盘复盘记录表
CREATE TABLE market_review (
                               id BIGSERIAL PRIMARY KEY,
                               review_date VARCHAR(10) NOT NULL,
                               review_name VARCHAR(100) NOT NULL,
                               review_time TIMESTAMP,
                               status VARCHAR(20) DEFAULT 'REVIEWING',
                               detail TEXT,
                               summary VARCHAR(50),
                               core_summary TEXT,
                               error_message TEXT,
                               create_time TIMESTAMP,
                               update_time TIMESTAMP,
                               version INTEGER DEFAULT 0,
                               creator VARCHAR(50),
                               modifier VARCHAR(50)
);

COMMENT ON TABLE market_review IS '大盘复盘记录表';
COMMENT ON COLUMN market_review.review_date IS '复盘日期，格式：2026-05-19';
COMMENT ON COLUMN market_review.review_name IS '复盘名称，格式：2026-05-19 A股复盘报告';
COMMENT ON COLUMN market_review.review_time IS '复盘时间';
COMMENT ON COLUMN market_review.status IS '复盘状态：REVIEWING-复盘中，COMPLETED-复盘完成，FAILED-复盘失败';
COMMENT ON COLUMN market_review.detail IS '复盘详情（Markdown格式）';
COMMENT ON COLUMN market_review.summary IS '总结，格式：小幅下跌/大幅上涨/小幅上涨/大幅下跌';
COMMENT ON COLUMN market_review.core_summary IS '核心总结';
COMMENT ON COLUMN market_review.error_message IS '错误信息';

CREATE UNIQUE INDEX idx_market_review_date ON market_review(review_date);

-- ----------------------------
-- 问一问功能：对话会话表
-- ----------------------------
DROP SEQUENCE IF EXISTS "chat_session_id_seq";
CREATE SEQUENCE "chat_session_id_seq"
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807
    MINVALUE 1
    CACHE 1
    NO CYCLE;

DROP TABLE IF EXISTS "chat_session";
CREATE TABLE "chat_session"
(
    id            BIGINT DEFAULT NEXTVAL('chat_session_id_seq'::REGCLASS) NOT NULL,
    session_name  VARCHAR(200)                                             NOT NULL,
    user_id       BIGINT                                                   NOT NULL,
    total_tokens  INTEGER      DEFAULT 0                                   NOT NULL,
    message_count INTEGER      DEFAULT 0                                   NOT NULL,
    create_time   TIMESTAMP    DEFAULT NOW()                               NOT NULL,
    update_time   TIMESTAMP,
    version       INTEGER      DEFAULT 0                                   NOT NULL,
    creator       VARCHAR(50),
    modifier      VARCHAR(50),
    CONSTRAINT chat_session_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

CREATE INDEX idx_chat_session_user_id ON public.chat_session USING btree (user_id);
CREATE INDEX idx_chat_session_update_time ON public.chat_session USING btree (update_time);

COMMENT ON TABLE chat_session IS '对话会话表';
COMMENT ON COLUMN chat_session.id IS '主键ID';
COMMENT ON COLUMN chat_session.session_name IS '会话名称';
COMMENT ON COLUMN chat_session.user_id IS '用户ID';
COMMENT ON COLUMN chat_session.total_tokens IS '累计消耗Token数';
COMMENT ON COLUMN chat_session.message_count IS '问答数量';
COMMENT ON COLUMN chat_session.create_time IS '创建时间';
COMMENT ON COLUMN chat_session.update_time IS '更新时间';
COMMENT ON COLUMN chat_session.version IS '版本号（乐观锁）';
COMMENT ON COLUMN chat_session.creator IS '创建人';
COMMENT ON COLUMN chat_session.modifier IS '修改人';

ALTER TABLE "chat_session" OWNER TO postgres;

-- ----------------------------
-- 问一问功能：对话消息表
-- ----------------------------
DROP SEQUENCE IF EXISTS "chat_message_id_seq";
CREATE SEQUENCE "chat_message_id_seq"
    START WITH 1
    INCREMENT BY 1
    MAXVALUE 9223372036854775807
    MINVALUE 1
    CACHE 1
    NO CYCLE;

DROP TABLE IF EXISTS "chat_message";
CREATE TABLE "chat_message"
(
    id            BIGINT DEFAULT NEXTVAL('chat_message_id_seq'::REGCLASS) NOT NULL,
    session_id    BIGINT                                                   NOT NULL,
    role          VARCHAR(20)                                              NOT NULL,
    content       TEXT                                                     NOT NULL,
    tokens        INTEGER      DEFAULT 0                                   NOT NULL,
    stock_code    VARCHAR(20),
    stock_name    VARCHAR(100),
    create_time   TIMESTAMP    DEFAULT NOW()                               NOT NULL,
    update_time   TIMESTAMP,
    version       INTEGER      DEFAULT 0                                   NOT NULL,
    creator       VARCHAR(50),
    modifier      VARCHAR(50),
    CONSTRAINT chat_message_pkey PRIMARY KEY (id)
) TABLESPACE pg_default;

CREATE INDEX idx_chat_message_session_id ON public.chat_message USING btree (session_id);
CREATE INDEX idx_chat_message_create_time ON public.chat_message USING btree (create_time);

COMMENT ON TABLE chat_message IS '对话消息表';
COMMENT ON COLUMN chat_message.id IS '主键ID';
COMMENT ON COLUMN chat_message.session_id IS '会话ID';
COMMENT ON COLUMN chat_message.role IS '角色：user-用户，assistant-AI助手';
COMMENT ON COLUMN chat_message.content IS '消息内容';
COMMENT ON COLUMN chat_message.tokens IS '消耗Token数';
COMMENT ON COLUMN chat_message.stock_code IS '关联股票代码';
COMMENT ON COLUMN chat_message.stock_name IS '关联股票名称';
COMMENT ON COLUMN chat_message.create_time IS '创建时间';
COMMENT ON COLUMN chat_message.update_time IS '更新时间';
COMMENT ON COLUMN chat_message.version IS '版本号（乐观锁）';
COMMENT ON COLUMN chat_message.creator IS '创建人';
COMMENT ON COLUMN chat_message.modifier IS '修改人';

ALTER TABLE "chat_message" OWNER TO postgres;






