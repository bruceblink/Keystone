alter table sys_user
    add column external_user_id varchar(128) null comment '外部用户ID' after external_subject;

create unique index uk_sys_user_external_user_id on sys_user (external_user_id);
