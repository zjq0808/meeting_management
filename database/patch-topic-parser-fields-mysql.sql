alter table t_meeting_topic
    modify column report_dept_id varchar(2000) null comment '汇报部门ID，多个以逗号分隔',
    modify column report_dept_name varchar(2000) null comment '汇报部门名称，多个以逗号分隔';

set @notice_sql = (
    select if(
        exists (
            select 1
            from information_schema.columns
            where table_schema = database()
              and table_name = 't_meeting_topic'
              and column_name = 'notice'
        ),
        'select 1',
        'alter table t_meeting_topic add column notice varchar(2000) null comment ''Word解析出的议题通知标题'' after conclusion'
    )
);
prepare notice_stmt from @notice_sql;
execute notice_stmt;
deallocate prepare notice_stmt;

set @project_code_sql = (
    select if(
        exists (
            select 1
            from information_schema.columns
            where table_schema = database()
              and table_name = 't_meeting_topic'
              and column_name = 'project_code'
        ),
        'select 1',
        'alter table t_meeting_topic add column project_code varchar(200) null comment ''Word解析出的项目编号'' after notice'
    )
);
prepare project_code_stmt from @project_code_sql;
execute project_code_stmt;
deallocate prepare project_code_stmt;
