set names utf8mb4;

create table if not exists t_meeting_operation_log (
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
