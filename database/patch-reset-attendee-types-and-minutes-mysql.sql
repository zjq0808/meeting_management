delete from t_meeting_attendee;

alter table t_meeting_attendee
    add column selected_source varchar(32) not null comment 'ADMIN/DEPARTMENT' after selected_by,
    add column selected_dept_id varchar(500) null comment '选择人所属部门ID' after selected_source;

alter table t_meeting_topic
    drop column if exists minutes,
    modify column summary text null comment '会议纪要';
