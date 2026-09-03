alter table users
    add column if not exists privacy_agreed boolean not null default false;

alter table users
    add column if not exists privacy_agreed_time timestamp;

comment on column users.privacy_agreed is '是否同意隐私政策';

comment on column users.privacy_agreed_time is '同意隐私政策时间';
