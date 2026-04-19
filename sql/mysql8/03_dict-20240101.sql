use agileboot;

-- ----------------------------
-- 字典类型表
-- ----------------------------
create table sys_dict_type
(
    dict_id     bigint auto_increment comment '字典主键'
        primary key,
    dict_name   varchar(100) default '' not null comment '字典名称',
    dict_type   varchar(100) default '' not null comment '字典类型',
    status      tinyint(1)   default 1  not null comment '状态（1正常 0停用）',
    remark      varchar(500)            null comment '备注',
    creator_id  bigint                  null comment '创建者ID',
    updater_id  bigint                  null comment '更新者ID',
    create_time datetime                null comment '创建时间',
    update_time datetime                null comment '更新时间',
    deleted     tinyint(1)   default 0  not null comment '逻辑删除',
    constraint dict_type_uniq_idx unique (dict_type)
) comment '字典类型表';

-- ----------------------------
-- 字典数据表
-- ----------------------------
create table sys_dict_data
(
    dict_code   bigint auto_increment comment '字典编码'
        primary key,
    dict_type   varchar(100) default '' not null comment '字典类型',
    dict_label  varchar(100) default '' not null comment '字典标签',
    dict_value  varchar(100) default '' not null comment '字典键值',
    dict_sort   int          default 0  not null comment '字典排序',
    is_default  tinyint(1)   default 0  not null comment '是否默认（1是 0否）',
    css_class   varchar(100)            null comment '样式属性（其他样式扩展）',
    list_class  varchar(100)            null comment '表格回显样式',
    status      tinyint(1)   default 1  not null comment '状态（1正常 0停用）',
    remark      varchar(500)            null comment '备注',
    creator_id  bigint                  null comment '创建者ID',
    updater_id  bigint                  null comment '更新者ID',
    create_time datetime                null comment '创建时间',
    update_time datetime                null comment '更新时间',
    deleted     tinyint(1)   default 0  not null comment '逻辑删除',
    index idx_dict_type (dict_type)
) comment '字典数据表';

-- ----------------------------
-- 初始数据：常用字典类型
-- ----------------------------
INSERT INTO sys_dict_type (dict_id, dict_name, dict_type, status, remark, creator_id, updater_id, create_time, update_time, deleted)
VALUES (1, '用户性别', 'sys_user_sex', 1, '用户性别列表', NULL, NULL, NOW(), NOW(), 0),
       (2, '菜单状态', 'sys_show_hide', 1, '菜单状态列表', NULL, NULL, NOW(), NOW(), 0),
       (3, '系统开关', 'sys_normal_disable', 1, '系统开关列表', NULL, NULL, NOW(), NOW(), 0),
       (4, '任务状态', 'sys_job_status', 1, '任务状态列表', NULL, NULL, NOW(), NOW(), 0),
       (5, '系统是否', 'sys_yes_no', 1, '系统是否列表', NULL, NULL, NOW(), NOW(), 0),
       (6, '通知类型', 'sys_notice_type', 1, '通知类型列表', NULL, NULL, NOW(), NOW(), 0),
       (7, '通知状态', 'sys_notice_status', 1, '通知状态列表', NULL, NULL, NOW(), NOW(), 0),
       (8, '操作类型', 'sys_oper_type', 1, '操作类型列表', NULL, NULL, NOW(), NOW(), 0),
       (9, '登录状态', 'sys_common_status', 1, '登录状态列表', NULL, NULL, NOW(), NOW(), 0);

-- ----------------------------
-- 初始数据：字典数据
-- ----------------------------
INSERT INTO sys_dict_data (dict_type, dict_label, dict_value, dict_sort, is_default, list_class, status, remark, creator_id, updater_id, create_time, update_time, deleted)
VALUES
-- 用户性别
('sys_user_sex', '男', '0', 1, 0, '', 1, '性别男', NULL, NULL, NOW(), NOW(), 0),
('sys_user_sex', '女', '1', 2, 0, '', 1, '性别女', NULL, NULL, NOW(), NOW(), 0),
('sys_user_sex', '未知', '2', 3, 1, '', 1, '性别未知', NULL, NULL, NOW(), NOW(), 0),
-- 菜单状态
('sys_show_hide', '显示', '0', 1, 1, 'primary', 1, '显示菜单', NULL, NULL, NOW(), NOW(), 0),
('sys_show_hide', '隐藏', '1', 2, 0, 'danger', 1, '隐藏菜单', NULL, NULL, NOW(), NOW(), 0),
-- 系统开关
('sys_normal_disable', '正常', '1', 1, 1, 'primary', 1, '正常状态', NULL, NULL, NOW(), NOW(), 0),
('sys_normal_disable', '停用', '0', 2, 0, 'danger', 1, '停用状态', NULL, NULL, NOW(), NOW(), 0),
-- 系统是否
('sys_yes_no', '是', '1', 1, 1, 'primary', 1, '系统默认是', NULL, NULL, NOW(), NOW(), 0),
('sys_yes_no', '否', '0', 2, 0, 'danger', 1, '系统默认否', NULL, NULL, NOW(), NOW(), 0),
-- 通知类型
('sys_notice_type', '通知', '1', 1, 1, 'warning', 1, '通知', NULL, NULL, NOW(), NOW(), 0),
('sys_notice_type', '公告', '2', 2, 0, 'success', 1, '公告', NULL, NULL, NOW(), NOW(), 0),
-- 通知状态
('sys_notice_status', '正常', '0', 1, 1, 'primary', 1, '正常状态', NULL, NULL, NOW(), NOW(), 0),
('sys_notice_status', '关闭', '1', 2, 0, 'danger', 1, '关闭状态', NULL, NULL, NOW(), NOW(), 0),
-- 操作类型
('sys_oper_type', '其他', '0', 1, 0, 'info', 1, '其他操作', NULL, NULL, NOW(), NOW(), 0),
('sys_oper_type', '新增', '1', 2, 0, 'info', 1, '新增操作', NULL, NULL, NOW(), NOW(), 0),
('sys_oper_type', '修改', '2', 3, 0, 'info', 1, '修改操作', NULL, NULL, NOW(), NOW(), 0),
('sys_oper_type', '删除', '3', 4, 0, 'danger', 1, '删除操作', NULL, NULL, NOW(), NOW(), 0),
('sys_oper_type', '导出', '4', 5, 0, 'warning', 1, '导出操作', NULL, NULL, NOW(), NOW(), 0),
('sys_oper_type', '导入', '5', 6, 0, 'warning', 1, '导入操作', NULL, NULL, NOW(), NOW(), 0),
-- 登录状态
('sys_common_status', '成功', '0', 1, 0, 'primary', 1, '正常状态', NULL, NULL, NOW(), NOW(), 0),
('sys_common_status', '失败', '1', 2, 0, 'danger', 1, '停用状态', NULL, NULL, NOW(), NOW(), 0);
