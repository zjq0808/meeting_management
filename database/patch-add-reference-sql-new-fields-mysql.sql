alter table t_meeting_main
    add column group_code varchar(32) null comment '应用分组标识（不同分组数据隔离）' after updated_time;

alter table t_meeting_topic
    add column `type` varchar(100) null comment '项目类型，例如重大项目、重大决策等' after topic_type,
    add column is_notice int null comment '是否推送消息（1、是2、否）' after report_dept_signed,
    add column group_code varchar(32) null comment '应用分组标识（不同分组数据隔离）' after updated_at;

alter table t_meeting_uploaded_file
    add column file_path varchar(255) not null default '' comment '文件大小（字节）' after file_name;
