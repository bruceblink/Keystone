ALTER TABLE sys_user
    ADD COLUMN external_subject varchar(128) NULL COMMENT '外部认证主体标识' AFTER is_admin;

ALTER TABLE sys_user
    ADD UNIQUE INDEX uk_sys_user_external_subject (external_subject);
