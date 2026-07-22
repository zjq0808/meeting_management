-- MySQL full initialization script for the meeting management system.
-- This file is generated from the fields currently read or written by Mapper XML and service logic.

set names utf8mb4;
set foreign_key_checks = 0;

drop table if exists t_meeting_uploaded_file;
drop table if exists t_meeting_operation_log;
drop table if exists t_meeting_notification;
drop table if exists t_meeting_attendee;
drop table if exists t_meeting_topic;
drop table if exists t_meeting_main;
drop table if exists t_meeting_authority;
drop table if exists t_wxdept_users;
drop table if exists t_wxdept;

set foreign_key_checks = 1;

create table t_wxdept (
    f_dept_id varchar(700) not null comment '部门ID，来自企业微信部门ID',
    f_dept_nm varchar(500) not null comment '部门名称',
    f_order_index int null comment '部门排序号，数值越小越靠前',
    f_parent_dept_id varchar(700) null comment '上级部门ID',
    f_create_time datetime not null default current_timestamp comment '部门同步或创建时间',
    f_pathids varchar(2000) null comment '部门路径ID，多个层级按同步数据格式保存',
    f_pathnames varchar(2000) null comment '部门路径名称，多个层级按同步数据格式保存',
    f_tel varchar(100) null comment '部门联系电话',
    f_deptnm_alias varchar(500) null comment '部门别名',
    primary key (f_dept_id),
    key idx_wxdept_order (f_order_index, f_dept_id),
    key idx_wxdept_parent (f_parent_dept_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='企业微信部门表';

create table t_wxdept_users (
    f_user_id varchar(500) not null comment '用户ID，来自企业微信用户ID，也是系统登录ID',
    f_user_name varchar(500) not null comment '用户姓名',
    f_dept_id varchar(700) null comment '所属部门ID，关联t_wxdept.f_dept_id',
    f_phone varchar(100) null comment '手机号',
    f_create_time datetime not null default current_timestamp comment '用户同步或创建时间',
    f_isleader varchar(10) null comment '企业微信部门负责人标识',
    f_position varchar(200) null comment '职务或岗位',
    f_email varchar(200) null comment '邮箱',
    f_gender varchar(10) null comment '性别',
    f_avatar varchar(1000) null comment '头像URL',
    f_english_name varchar(200) null comment '英文名',
    f_status varchar(20) null comment '企业微信用户状态',
    f_qr_code varchar(1000) null comment '企业微信个人二维码',
    f_telephone varchar(100) null comment '办公电话',
    f_isQianWei varchar(10) null comment '企微同步预留字段',
    f_center_id varchar(100) null comment '中心ID预留字段',
    f_xndept_id varchar(100) null comment '虚拟部门ID预留字段',
    f_desc varchar(1000) null comment '用户描述预留字段',
    f_emp_typ varchar(50) null comment '员工类型预留字段',
    f_emp_status varchar(50) null comment '员工状态预留字段',
    f_tb varchar(50) null comment '同步批次或来源预留字段',
    f_tags varchar(1000) null comment '用户标签预留字段',
    f_enable varchar(10) null comment '是否启用，0表示禁用，其它值或空值表示可用',
    needauth_user varchar(10) null comment '企微同步权限预留字段：用户',
    needauth_dept varchar(10) null comment '企微同步权限预留字段：部门',
    needauth_tag varchar(10) null comment '企微同步权限预留字段：标签',
    primary key (f_user_id),
    key idx_wx_user_dept (f_dept_id),
    key idx_wx_user_name (f_user_name),
    constraint fk_wx_user_dept foreign key (f_dept_id) references t_wxdept (f_dept_id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='企业微信用户表';

create table t_meeting_authority (
    id bigint not null auto_increment comment '主键ID',
    user_id varchar(500) not null comment '用户ID，关联t_wxdept_users.f_user_id',
    user_name varchar(500) not null comment '用户姓名快照',
    dept_id varchar(700) null comment '权限所属部门ID，部门秘书按该字段匹配议题部门',
    authority tinyint not null comment '角色权限：1=ADMIN会议管理员，2=LEADER科组长，3=SECRETARY部门秘书',
    group_code varchar(100) not null default 'DEFAULT' comment '权限分组编码，当前业务默认DEFAULT',
    create_time datetime not null default current_timestamp comment '授权创建时间',
    primary key (id),
    unique key uk_meeting_authority_user_dept_group (user_id, dept_id, group_code),
    key idx_authority_group_user (group_code, user_id),
    key idx_authority_dept_group (dept_id, group_code),
    key idx_authority_value (authority),
    constraint fk_authority_user foreign key (user_id) references t_wxdept_users (f_user_id) on update cascade on delete cascade,
    constraint fk_authority_dept foreign key (dept_id) references t_wxdept (f_dept_id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='会议系统角色权限表';

create table t_meeting_main (
    id bigint not null auto_increment comment '会议ID',
    meeting_date varchar(50) not null comment '会议日期，前端按yyyy-MM-dd传入',
    meeting_time varchar(50) not null comment '会议时间，前端按HH:mm或时间文本传入',
    period_no varchar(100) null comment '会议期次',
    location varchar(500) null comment '会议地点',
    leaders varchar(1000) null comment '参会或主持领导展示文本',
    content varchar(2000) null comment '会议内容摘要',
    status varchar(32) not null default 'DRAFT' comment '会议状态：DRAFT草稿，PUBLISHED已发布，IN_PROGRESS进行中，FINISHED已结束',
    creater varchar(500) null comment '创建人用户ID',
    created_time datetime not null default current_timestamp comment '创建时间',
    updated_time datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (id),
    key idx_meeting_status (status),
    key idx_meeting_date (meeting_date, id),
    key idx_meeting_creater (creater),
    constraint fk_meeting_creator foreign key (creater) references t_wxdept_users (f_user_id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='会议主表';

create table t_meeting_topic (
    id bigint not null auto_increment comment '议题ID',
    meeting_id bigint not null comment '会议ID，关联t_meeting_main.id',
    topic_type varchar(100) null comment '议题类型，例如三重一大、重大项目等',
    title varchar(1000) not null comment '议题标题',
    report_dept_id varchar(2000) null comment '汇报部门ID，支持多个部门，逗号分隔',
    report_dept_name varchar(2000) null comment '汇报部门名称快照，支持多个部门，逗号分隔',
    participant_dept_id varchar(2000) null comment '参会部门ID，支持多个部门，逗号分隔',
    participant_dept_name varchar(2000) null comment '参会部门名称快照，支持多个部门，逗号分隔',
    summary text null comment '议题摘要或会议纪要',
    notice varchar(2000) null comment '从Word解析出的通知或议题说明',
    project_code varchar(200) null comment '从Word解析出的项目编号',
    sort_no int not null default 1 comment '议题排序号，会议内按该字段升序展示',
    status varchar(32) not null default 'WAITING' comment '议题状态：WAITING待开始，RUNNING进行中，FINISHED已结束',
    start_time datetime null comment '议题实际开始时间',
    end_time datetime null comment '议题实际结束时间',
    actual_minutes int null comment '议题实际耗时，单位分钟',
    conclusion text null comment '议题结论',
    report_dept_signed tinyint not null default 0 comment '汇报部门是否已签到：0未签到，1已签到',
    created_at datetime not null default current_timestamp comment '创建时间',
    updated_at datetime not null default current_timestamp on update current_timestamp comment '更新时间',
    primary key (id),
    key idx_meeting_topic_meeting (meeting_id, sort_no, id),
    key idx_topic_status (meeting_id, status),
    key idx_topic_report_dept (report_dept_id(255)),
    key idx_topic_participant_dept (participant_dept_id(255)),
    constraint fk_topic_meeting foreign key (meeting_id) references t_meeting_main (id) on update cascade on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='会议议题表';

create table t_meeting_attendee (
    id bigint not null auto_increment comment '参会记录ID',
    meeting_id bigint not null comment '会议ID，关联t_meeting_main.id',
    topic_id bigint not null comment '议题ID，关联t_meeting_topic.id',
    user_id varchar(500) not null comment '参会人员用户ID，关联t_wxdept_users.f_user_id',
    user_name varchar(500) not null comment '参会人员姓名快照',
    attendee_type varchar(32) not null comment '参会类型：REPORT汇报人，PARTAKE参会人',
    selected_by varchar(500) null comment '选择该参会人的操作人用户ID',
    selected_source varchar(32) not null default 'ADMIN' comment '选择来源：ADMIN管理员选择，DEPARTMENT部门选择',
    selected_dept_id varchar(500) null comment '部门来源记录所属部门ID',
    created_at datetime not null default current_timestamp comment '记录创建时间',
    confirm_status varchar(32) not null default 'PENDING' comment '参会确认状态：PENDING待确认，CONFIRMED已确认',
    confirmed_by varchar(500) null comment '确认人用户ID',
    confirmed_at datetime null comment '确认时间',
    sign_status varchar(32) not null default 'UNSIGNED' comment '签到状态：UNSIGNED未签到，SIGNED已签到',
    signed_at datetime null comment '签到时间',
    primary key (id),
    unique key uk_attendee_topic_user_type (topic_id, user_id, attendee_type),
    key idx_attendee_meeting (meeting_id),
    key idx_attendee_topic (topic_id),
    key idx_attendee_user (user_id, sign_status),
    key idx_attendee_confirm (user_id, confirm_status),
    key idx_attendee_selected (meeting_id, selected_by, confirm_status),
    key idx_attendee_selected_dept (meeting_id, selected_source, selected_dept_id, confirm_status),
    constraint fk_attendee_meeting foreign key (meeting_id) references t_meeting_main (id) on update cascade on delete cascade,
    constraint fk_attendee_topic foreign key (topic_id) references t_meeting_topic (id) on update cascade on delete cascade,
    constraint fk_attendee_user foreign key (user_id) references t_wxdept_users (f_user_id) on update cascade on delete cascade
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='议题参会人员与签到表';

create table t_meeting_notification (
    id bigint not null auto_increment comment '通知ID',
    meeting_id bigint null comment '会议ID，关联t_meeting_main.id',
    topic_id bigint null comment '议题ID，关联t_meeting_topic.id',
    title varchar(500) not null comment '通知标题',
    msg_body text null comment '通知内容',
    tousers text null comment '接收人用户ID，当前按文本保存，可存单个或多个ID',
    send_time datetime null comment '发送时间',
    job_id varchar(200) null comment '外部发送任务ID或本地模拟任务ID',
    created_at datetime not null default current_timestamp comment '记录创建时间',
    primary key (id),
    key idx_notice_meeting (meeting_id),
    key idx_notice_topic (topic_id),
    key idx_notice_created (created_at),
    constraint fk_notice_meeting foreign key (meeting_id) references t_meeting_main (id) on update cascade on delete set null,
    constraint fk_notice_topic foreign key (topic_id) references t_meeting_topic (id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='会议通知记录表';

create table t_meeting_operation_log (
    id bigint not null auto_increment comment '操作记录ID',
    meeting_id bigint null comment '会议ID，关联t_meeting_main.id',
    topic_id bigint null comment '议题ID，关联t_meeting_topic.id',
    operation_type varchar(64) not null comment '操作类型：SELECT_REPORTER选择汇报人，SELECT_PARTICIPANT选择参会人，SHARE_TOPIC分享议题',
    operation_name varchar(100) not null comment '操作名称，用于页面展示',
    operator_id varchar(500) null comment '操作人用户ID',
    operator_name varchar(500) null comment '操作人姓名快照',
    operator_role varchar(32) null comment '操作人角色：ADMIN、SECRETARY、LEADER等',
    operator_dept_id varchar(700) null comment '操作人部门ID快照',
    operator_dept_name varchar(500) null comment '操作人部门名称快照',
    target_user_ids text null comment '目标人员用户ID，多个用逗号分隔',
    target_user_names text null comment '目标人员姓名，多个用逗号分隔',
    target_dept_ids text null comment '目标人员部门ID，多个用逗号分隔',
    target_dept_names text null comment '目标人员部门名称，多个用逗号分隔',
    operation_content text null comment '操作内容描述',
    created_at datetime not null default current_timestamp comment '操作时间',
    primary key (id),
    key idx_operation_meeting (meeting_id, created_at),
    key idx_operation_topic (topic_id, created_at),
    key idx_operation_operator (operator_id, created_at),
    key idx_operation_type (operation_type, created_at),
    constraint fk_operation_meeting foreign key (meeting_id) references t_meeting_main (id) on update cascade on delete set null,
    constraint fk_operation_topic foreign key (topic_id) references t_meeting_topic (id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='会议操作记录表';

create table t_meeting_uploaded_file (
    id bigint not null auto_increment comment '上传记录ID',
    meeting_id bigint null comment '会议ID，导入到会议时关联t_meeting_main.id',
    file_name varchar(500) null comment '上传文件名',
    file_size bigint null comment '文件大小，单位字节',
    parser_status varchar(32) null comment '解析状态：SUCCESS成功，FAILED失败',
    parser_message varchar(1000) null comment '解析结果或失败原因',
    created_at datetime not null default current_timestamp comment '记录创建时间',
    primary key (id),
    key idx_upload_meeting (meeting_id),
    key idx_upload_created (created_at),
    constraint fk_upload_meeting foreign key (meeting_id) references t_meeting_main (id) on update cascade on delete set null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_unicode_ci comment='议题Word导入记录表';

insert into t_wxdept(f_dept_id, f_dept_nm, f_order_index, f_parent_dept_id, f_pathids, f_pathnames) values
('D001', '办公室', 1, null, 'D001', '办公室'),
('D002', '规划部', 2, null, 'D002', '规划部'),
('D003', '数据中心', 3, null, 'D003', '数据中心'),
('D004', '安全部', 4, null, 'D004', '安全部'),
('D005', '网络运维部', 5, null, 'D005', '网络运维部');

insert into t_wxdept_users(f_user_id, f_user_name, f_dept_id, f_phone, f_position, f_email, f_enable) values
('U0001', '系统管理员', 'D001', '13800000001', '会议管理员', 'admin@example.com', '1'),
('U0002', '规划部秘书', 'D002', '13800000002', '部门秘书', 'secretary-plan@example.com', '1'),
('U0003', '数据中心秘书', 'D003', '13800000003', '部门秘书', 'secretary-data@example.com', '1'),
('U0004', '规划部科组长', 'D002', '13800000004', '科组长', 'leader-plan@example.com', '1'),
('U0005', '数据中心科组长', 'D003', '13800000005', '科组长', 'leader-data@example.com', '1'),
('U0006', '规划部参会人', 'D002', '13800000006', '工程师', 'plan-user@example.com', '1'),
('U0007', '数据中心参会人', 'D003', '13800000007', '工程师', 'data-user@example.com', '1'),
('U0008', '安全部秘书', 'D004', '13800000008', '部门秘书', 'secretary-safe@example.com', '1'),
('U0009', '网络运维参会人', 'D005', '13800000009', '工程师', 'network-user@example.com', '1');

insert into t_meeting_authority(user_id, user_name, dept_id, authority, group_code) values
('U0001', '系统管理员', 'D001', 1, 'DEFAULT'),
('U0002', '规划部秘书', 'D002', 3, 'DEFAULT'),
('U0003', '数据中心秘书', 'D003', 3, 'DEFAULT'),
('U0004', '规划部科组长', 'D002', 2, 'DEFAULT'),
('U0005', '数据中心科组长', 'D003', 2, 'DEFAULT'),
('U0008', '安全部秘书', 'D004', 3, 'DEFAULT');
