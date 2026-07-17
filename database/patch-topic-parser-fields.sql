alter table t_meeting_topic alter column report_dept_id type varchar(2000);
alter table t_meeting_topic alter column report_dept_name type varchar(2000);
alter table t_meeting_topic add column if not exists notice varchar(2000);
alter table t_meeting_topic add column if not exists project_code varchar(200);
