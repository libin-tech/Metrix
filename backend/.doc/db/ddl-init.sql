create table users
(
    id                  bigserial
        primary key,
    username            varchar(50)                                     not null,
    password            varchar(255)                                    not null,
    email               varchar(100),
    role                varchar(20) default 'USER'::character varying   not null,
    is_active           boolean     default true                        not null,
    create_time         timestamp   default now()                       not null,
    update_time         timestamp,
    version             integer     default 0                           not null,
    creator             varchar(50),
    modifier            varchar(50),
    nickname            varchar(100),
    avatar              varchar(500),
    openid              varchar(100),
    privacy_agreed      boolean     default false,
    privacy_agreed_time timestamp,
    status              varchar(20) default 'NORMAL'::character varying not null,
    freeze_reason       varchar(500)
);

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

comment on column users.nickname is '微信昵称';

comment on column users.avatar is '微信头像URL';

comment on column users.openid is '微信OpenID（唯一标识）';

comment on column users.privacy_agreed is '是否同意隐私政策';

comment on column users.privacy_agreed_time is '同意隐私政策时间';

comment on column users.status is '用户状态：NORMAL-正常 FROZEN-冻结';

comment on column users.freeze_reason is '冻结备注';

alter table users
    owner to postgres;

create unique index idx_users_username
    on users (username);

create unique index idx_users_email
    on users (email);

create unique index idx_users_openid
    on users (openid)
    where (openid IS NOT NULL);

create table stock_analysis_record
(
    id                bigserial
        primary key,
    stock_code        varchar(20)                                        not null,
    stock_name        varchar(100)                                       not null,
    analysis_type     varchar(50)                                        not null,
    analysis_result   text,
    market_data       text,
    news_summary      text,
    create_time       timestamp   default now()                          not null,
    update_time       timestamp,
    version           integer     default 0                              not null,
    creator           varchar(50),
    modifier          varchar(50),
    status            varchar(20) default 'COMPLETED'::character varying not null,
    analysis_overview text,
    depth_data        text,
    klines_data       text,
    user_id           bigint
);

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

comment on column stock_analysis_record.user_id is '用户ID（数据隔离）';

alter table stock_analysis_record
    owner to postgres;

create index idx_analysis_stock_code
    on stock_analysis_record (stock_code);

create index idx_analysis_created_at
    on stock_analysis_record (create_time);

create index idx_stock_analysis_record_status
    on stock_analysis_record (status);

create index idx_analysis_user_id
    on stock_analysis_record (user_id);

create table market_data_config
(
    id               bigserial
        primary key,
    source_name      varchar(50)             not null,
    api_url          varchar(500)            not null,
    api_key          varchar(500),
    data_type        varchar(50)             not null,
    request_interval integer   default 60    not null,
    is_active        boolean   default true  not null,
    user_id          bigint                  not null default 0,
    create_time      timestamp default now() not null,
    update_time      timestamp,
    version          integer   default 0     not null,
    creator          varchar(50),
    modifier         varchar(50),
    timeout          integer   default 60    not null,
    remark           varchar(100)
);

comment on table market_data_config is '市场数据配置表';

comment on column market_data_config.id is '主键ID';

comment on column market_data_config.source_name is '源名称（TICKFLOW）';

comment on column market_data_config.api_url is 'API URL';

comment on column market_data_config.api_key is 'API密钥';

comment on column market_data_config.data_type is '数据类型';

comment on column market_data_config.request_interval is '请求间隔（秒）';

comment on column market_data_config.is_active is '是否激活';

comment on column market_data_config.user_id is '用户ID';

comment on column market_data_config.create_time is '创建时间';

comment on column market_data_config.update_time is '更新时间';

comment on column market_data_config.version is '版本号（乐观锁）';

comment on column market_data_config.creator is '创建人';

comment on column market_data_config.modifier is '修改人';

comment on column market_data_config.timeout is '请求超时时间（秒），默认60秒';

comment on column market_data_config.remark is '备注，限制100字以内';

alter table market_data_config
    owner to postgres;

create index idx_market_data_source
    on market_data_config (source_name);

create index idx_market_data_config_user_id
    on market_data_config (user_id);

create table news_source_config
(
    id               bigserial
        primary key,
    source_name      varchar(50)             not null,
    api_url          varchar(500)            not null,
    api_key          varchar(500),
    request_interval integer   default 60    not null,
    is_active        boolean   default true  not null,
    user_id          bigint                  not null default 0,
    create_time      timestamp default now() not null,
    update_time      timestamp,
    version          integer   default 0     not null,
    creator          varchar(50),
    modifier         varchar(50),
    timeout          integer   default 60    not null,
    remark           varchar(100)
);

comment on table news_source_config is '新闻源配置表';

comment on column news_source_config.id is '主键ID';

comment on column news_source_config.source_name is '源名称（BOCHA）';

comment on column news_source_config.api_url is 'API URL';

comment on column news_source_config.api_key is 'API密钥';

comment on column news_source_config.request_interval is '请求间隔（秒）';

comment on column news_source_config.is_active is '是否激活';

comment on column news_source_config.user_id is '用户ID';

comment on column news_source_config.create_time is '创建时间';

comment on column news_source_config.update_time is '更新时间';

comment on column news_source_config.version is '版本号（乐观锁）';

comment on column news_source_config.creator is '创建人';

comment on column news_source_config.modifier is '修改人';

comment on column news_source_config.timeout is '请求超时时间（秒），默认60秒';

comment on column news_source_config.remark is '备注，限制100字以内';

alter table news_source_config
    owner to postgres;

create index idx_news_source
    on news_source_config (source_name);

create index idx_news_source_config_user_id
    on news_source_config (user_id);

create table notification_config
(
    id           bigserial
        primary key,
    channel_type varchar(50)             not null,
    webhook_url  varchar(500)            not null,
    secret       varchar(500),
    is_active    boolean   default true  not null,
    user_id      bigint                  not null default 0,
    create_time  timestamp default now() not null,
    update_time  timestamp,
    version      integer   default 0     not null,
    creator      varchar(50),
    modifier     varchar(50)
);

comment on table notification_config is '通知配置表';

comment on column notification_config.id is '主键ID';

comment on column notification_config.channel_type is '渠道类型（FEISHU）';

comment on column notification_config.webhook_url is 'WebHook URL';

comment on column notification_config.secret is '密钥';

comment on column notification_config.is_active is '是否激活';

comment on column notification_config.user_id is '用户ID';

comment on column notification_config.create_time is '创建时间';

comment on column notification_config.update_time is '更新时间';

comment on column notification_config.version is '版本号（乐观锁）';

comment on column notification_config.creator is '创建人';

comment on column notification_config.modifier is '修改人';

alter table notification_config
    owner to postgres;

create index idx_notification_channel
    on notification_config (channel_type);

create index idx_notification_config_user_id
    on notification_config (user_id);

create table ai_model_config
(
    id           bigserial
        primary key,
    model_type   varchar(50)                    not null,
    model_name   varchar(100)                   not null,
    api_base_url varchar(500),
    api_key      varchar(500),
    temperature  double precision default 0.7,
    is_active    boolean          default true  not null,
    user_id      bigint                         not null default 0,
    create_time  timestamp        default now() not null,
    update_time  timestamp,
    version      integer          default 0     not null,
    creator      varchar(50),
    modifier     varchar(50),
    timeout      integer          default 120   not null
);

comment on table ai_model_config is 'AI模型配置表';

comment on column ai_model_config.id is '主键ID';

comment on column ai_model_config.model_type is '模型类型（OPENAI/OLLAMA/GEMINI）';

comment on column ai_model_config.model_name is '模型名称';

comment on column ai_model_config.api_base_url is 'API基础URL';

comment on column ai_model_config.api_key is 'API密钥';

comment on column ai_model_config.temperature is '温度参数';

comment on column ai_model_config.is_active is '是否激活';

comment on column ai_model_config.user_id is '用户ID';

comment on column ai_model_config.create_time is '创建时间';

comment on column ai_model_config.update_time is '更新时间';

comment on column ai_model_config.version is '版本号（乐观锁）';

comment on column ai_model_config.creator is '创建人';

comment on column ai_model_config.modifier is '修改人';

comment on column ai_model_config.timeout is '超时时间（秒），默认120秒';

alter table ai_model_config
    owner to postgres;

create index idx_ai_model_type
    on ai_model_config (model_type);

create index idx_ai_model_config_user_id
    on ai_model_config (user_id);

create table stock_basic
(
    id           bigserial
        primary key,
    ts_code      varchar(20)             not null,
    symbol       varchar(6)              not null,
    name         varchar(100)            not null,
    area         varchar(50),
    industry     varchar(100),
    cnspell      varchar(20),
    market       varchar(20),
    list_date    date,
    act_name     varchar(200),
    act_ent_type varchar(50),
    create_time  timestamp default now() not null,
    update_time  timestamp,
    version      integer   default 0     not null,
    creator      varchar(50),
    modifier     varchar(50)
);

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

alter table stock_basic
    owner to postgres;

create unique index idx_stock_basic_ts_code
    on stock_basic (ts_code);

create unique index idx_stock_basic_symbol
    on stock_basic (symbol);

create table broker_account
(
    id             bigserial
        primary key,
    broker_name    varchar(10)             not null,
    account_number varchar(30),
    remark         varchar(50),
    user_id        bigint                  not null default 0,
    create_time    timestamp default now() not null,
    update_time    timestamp,
    version        integer   default 0     not null,
    creator        varchar(50),
    modifier       varchar(50)
);

comment on table broker_account is '券商账户';

comment on column broker_account.id is '主键ID';

comment on column broker_account.broker_name is '券商名称';

comment on column broker_account.account_number is '券商账号';

comment on column broker_account.remark is '备注';

comment on column broker_account.user_id is '用户ID';

comment on column broker_account.create_time is '创建时间';

comment on column broker_account.update_time is '更新时间';

comment on column broker_account.version is '版本号（乐观锁）';

comment on column broker_account.creator is '创建人';

comment on column broker_account.modifier is '修改人';

alter table broker_account
    owner to postgres;

create index idx_broker_account_user_id
    on broker_account (user_id);

create table portfolio_holding
(
    id                bigserial
        primary key,
    account_id        bigint                  not null,
    stock_code        varchar(50)             not null,
    stock_name        varchar(100)            not null,
    cost              numeric(20, 4),
    quantity          numeric(20, 2),
    user_id           bigint                  not null default 0,
    cached_price      decimal(20, 4),
    cached_price_time timestamp,
    create_time       timestamp default now() not null,
    update_time       timestamp,
    version           integer   default 0     not null,
    creator           varchar(50),
    modifier          varchar(50)
);

comment on table portfolio_holding is '我的持仓表';

comment on column portfolio_holding.id is '主键ID';

comment on column portfolio_holding.account_id is '券商账户ID';

comment on column portfolio_holding.stock_code is '标的代码';

comment on column portfolio_holding.stock_name is '标的名称';

comment on column portfolio_holding.cost is '成本';

comment on column portfolio_holding.quantity is '数量';

comment on column portfolio_holding.user_id is '用户ID';

comment on column portfolio_holding.cached_price is '缓存的最新行情价格';

comment on column portfolio_holding.cached_price_time is '行情缓存时间';

comment on column portfolio_holding.create_time is '创建时间';

comment on column portfolio_holding.update_time is '更新时间';

comment on column portfolio_holding.version is '版本号（乐观锁）';

comment on column portfolio_holding.creator is '创建人';

comment on column portfolio_holding.modifier is '修改人';

alter table portfolio_holding
    owner to postgres;

create index idx_holding_account_id
    on portfolio_holding (account_id);

create index idx_holding_stock_code
    on portfolio_holding (stock_code);

create index idx_portfolio_holding_user_id
    on portfolio_holding (user_id);

create table market_review
(
    id            bigserial
        primary key,
    review_date   varchar(10)  not null,
    review_name   varchar(100) not null,
    review_time   timestamp,
    status        varchar(20) default 'REVIEWING'::character varying,
    detail        text,
    summary       varchar(50),
    core_summary  text,
    error_message text,
    user_id       bigint      default null,
    create_time   timestamp,
    update_time   timestamp,
    version       integer     default 0,
    creator       varchar(50),
    modifier      varchar(50)
);

comment on table market_review is '大盘复盘记录表';

comment on column market_review.review_date is '复盘日期，格式：2026-05-19';

comment on column market_review.review_name is '复盘名称，格式：2026-05-19A股复盘报告';

comment on column market_review.review_time is '复盘时间';

comment on column market_review.status is '复盘状态：REVIEWING-复盘中，COMPLETED-复盘完成，FAILED-复盘失败';

comment on column market_review.detail is '复盘详情（Markdown格式）';

comment on column market_review.summary is '总结，格式：小幅下跌/大幅上涨/小幅上涨/大幅下跌';

comment on column market_review.core_summary is '核心总结（500字内Markdown格式）';

comment on column market_review.error_message is '错误信息';

comment on column market_review.user_id is '用户ID，NULL表示系统生成';

comment on column market_review.create_time is '创建时间';

alter table market_review
    owner to postgres;

create unique index idx_market_review_date
    on market_review (review_date);

create index idx_market_review_user_id
    on market_review (user_id);

create table chat_session
(
    id            bigserial
        primary key,
    session_name  varchar(200)            not null,
    user_id       bigint                  not null,
    total_tokens  integer   default 0     not null,
    message_count integer   default 0     not null,
    create_time   timestamp default now() not null,
    update_time   timestamp,
    version       integer   default 0     not null,
    creator       varchar(50),
    modifier      varchar(50)
);

comment on table chat_session is '对话会话表';

comment on column chat_session.id is '主键ID';

comment on column chat_session.session_name is '会话名称';

comment on column chat_session.user_id is '用户ID';

comment on column chat_session.total_tokens is '累计消耗Token数';

comment on column chat_session.message_count is '问答数量';

comment on column chat_session.create_time is '创建时间';

comment on column chat_session.update_time is '更新时间';

comment on column chat_session.version is '版本号（乐观锁）';

comment on column chat_session.creator is '创建人';

comment on column chat_session.modifier is '修改人';

alter table chat_session
    owner to postgres;

create index idx_chat_session_user_id
    on chat_session (user_id);

create index idx_chat_session_update_time
    on chat_session (update_time);

create table chat_message
(
    id          bigserial
        primary key,
    session_id  bigint                  not null,
    role        varchar(20)             not null,
    content     text                    not null,
    tokens      integer   default 0     not null,
    stock_code  varchar(20),
    stock_name  varchar(100),
    user_id     bigint                  not null default 0,
    create_time timestamp default now() not null,
    update_time timestamp,
    version     integer   default 0     not null,
    creator     varchar(50),
    modifier    varchar(50),
    steps       text
);

comment on table chat_message is '对话消息表';

comment on column chat_message.id is '主键ID';

comment on column chat_message.session_id is '会话ID';

comment on column chat_message.role is '角色：user-用户，assistant-AI助手';

comment on column chat_message.content is '消息内容';

comment on column chat_message.tokens is '消耗Token数';

comment on column chat_message.stock_code is '关联股票代码';

comment on column chat_message.stock_name is '关联股票名称';

comment on column chat_message.user_id is '用户ID';

comment on column chat_message.create_time is '创建时间';

comment on column chat_message.update_time is '更新时间';

comment on column chat_message.version is '版本号（乐观锁）';

comment on column chat_message.creator is '创建人';

comment on column chat_message.modifier is '修改人';

comment on column chat_message.steps is 'AI分析步骤记录（JSON数组），包含每步耗时和状态';

alter table chat_message
    owner to postgres;

create index idx_chat_message_session_id
    on chat_message (session_id);

create index idx_chat_message_create_time
    on chat_message (create_time);

create index idx_chat_message_user_id
    on chat_message (user_id);


create table system_role
(
    id          bigserial
        primary key,
    role_code   varchar(50)                                     not null
        constraint system_role_code_unique
            unique,
    role_name   varchar(100)                                    not null,
    description varchar(255),
    is_system   boolean     default false                       not null,
    status      varchar(20) default 'ACTIVE'::character varying not null,
    sort_order  integer     default 0,
    create_time timestamp   default now()                       not null,
    update_time timestamp,
    version     integer     default 0                           not null,
    creator     varchar(50),
    modifier    varchar(50)
);

comment on table system_role is '系统角色表';

comment on column system_role.role_code is '角色编码（如 ADMIN/USER）';

comment on column system_role.role_name is '角色名称（如 管理员/普通用户）';

comment on column system_role.description is '角色描述';

comment on column system_role.is_system is '是否系统内置（系统角色不可删除）';

comment on column system_role.status is '状态：ACTIVE-启用 DISABLED-禁用';

comment on column system_role.sort_order is '排序号';

alter table system_role
    owner to postgres;

create table system_menu
(
    id              bigserial
        primary key,
    parent_id       bigint,
    menu_name       varchar(100)                                    not null,
    permission_code varchar(100)
        constraint system_menu_permission_code_unique
            unique,
    menu_type       varchar(20)                                     not null,
    path            varchar(200),
    component       varchar(200),
    icon            varchar(100),
    sort_order      integer     default 0,
    status          varchar(20) default 'ACTIVE'::character varying not null,
    visible         boolean     default true                        not null,
    create_time     timestamp   default now()                       not null,
    update_time     timestamp,
    version         integer     default 0                           not null,
    creator         varchar(50),
    modifier        varchar(50)
);

comment on table system_menu is '系统菜单/按钮表';

comment on column system_menu.parent_id is '父菜单ID（目录/菜单的上级）';

comment on column system_menu.menu_name is '菜单名称';

comment on column system_menu.permission_code is '权限标识（如 system:user:list）';

comment on column system_menu.menu_type is '类型：DIRECTORY-目录 MENU-菜单 BUTTON-按钮';

comment on column system_menu.path is '前端路由路径';

comment on column system_menu.component is '前端组件路径';

comment on column system_menu.icon is '菜单图标';

comment on column system_menu.sort_order is '排序号';

comment on column system_menu.status is '状态：ACTIVE-启用 DISABLED-禁用';

comment on column system_menu.visible is '是否可见（用于控制是否在菜单树中显示）';

alter table system_menu
    owner to postgres;

create table system_api
(
    id              bigserial
        primary key,
    api_name        varchar(100)                                    not null,
    api_path        varchar(200)                                    not null,
    http_method     varchar(10)                                     not null,
    permission_code varchar(100)
        constraint system_api_permission_code_unique
            unique,
    description     varchar(255),
    status          varchar(20) default 'ACTIVE'::character varying not null,
    create_time     timestamp   default now()                       not null,
    update_time     timestamp,
    version         integer     default 0                           not null,
    creator         varchar(50),
    modifier        varchar(50)
);

comment on table system_api is '系统接口权限表';

comment on column system_api.api_name is '接口名称';

comment on column system_api.api_path is '接口路径（如 /api/admin/users）';

comment on column system_api.http_method is 'HTTP 方法（GET/POST/PUT/DELETE）';

comment on column system_api.permission_code is '权限标识（如 system:user:list），每个端点独立';

comment on column system_api.description is '接口描述';

comment on column system_api.status is '状态：ACTIVE-启用 DISABLED-禁用';

alter table system_api
    owner to postgres;

create table system_role_menu
(
    id          bigserial
        primary key,
    role_id     bigint                  not null,
    menu_id     bigint                  not null,
    create_time timestamp default now() not null,
    update_time timestamp,
    version     integer   default 0     not null,
    creator     varchar(50),
    modifier    varchar(50),
    constraint system_role_menu_unique
        unique (role_id, menu_id)
);

comment on table system_role_menu is '角色-菜单关联表';

comment on column system_role_menu.role_id is '角色ID';

comment on column system_role_menu.menu_id is '菜单ID';

alter table system_role_menu
    owner to postgres;

create table system_role_api
(
    id          bigserial
        primary key,
    role_id     bigint                  not null,
    api_id      bigint                  not null,
    create_time timestamp default now() not null,
    update_time timestamp,
    version     integer   default 0     not null,
    creator     varchar(50),
    modifier    varchar(50),
    constraint system_role_api_unique
        unique (role_id, api_id)
);

comment on table system_role_api is '角色-接口关联表';

comment on column system_role_api.role_id is '角色ID';

comment on column system_role_api.api_id is '接口ID';

alter table system_role_api
    owner to postgres;

create table system_menu_api
(
    id          bigserial
        primary key,
    menu_id     bigint                  not null,
    api_id      bigint                  not null,
    create_time timestamp default now() not null,
    update_time timestamp,
    version     integer   default 0     not null,
    creator     varchar(50),
    modifier    varchar(50),
    constraint system_menu_api_unique
        unique (menu_id, api_id)
);

comment on table system_menu_api is '菜单-接口关联表（前端权限关联后端接口）';

comment on column system_menu_api.menu_id is '菜单ID';

comment on column system_menu_api.api_id is '接口ID';

alter table system_menu_api
    owner to postgres;

create table system_user_role
(
    id          bigserial
        primary key,
    user_id     bigint                  not null,
    role_id     bigint                  not null,
    create_time timestamp default now() not null,
    update_time timestamp,
    version     integer   default 0     not null,
    creator     varchar(50),
    modifier    varchar(50),
    constraint system_user_role_unique
        unique (user_id, role_id)
);

comment on table system_user_role is '用户-角色关联表';

comment on column system_user_role.user_id is '用户ID';

comment on column system_user_role.role_id is '角色ID';

alter table system_user_role
    owner to postgres;

