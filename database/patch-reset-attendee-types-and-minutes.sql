delete from t_meeting_attendee;

alter table t_meeting_attendee add column if not exists selected_source varchar(32) not null;
alter table t_meeting_attendee add column if not exists selected_dept_id varchar(500);
alter table t_meeting_topic drop column if exists minutes;
comment on column t_meeting_topic.summary is '会议纪要';
