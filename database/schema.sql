drop table if exists t_meeting_uploaded_file;
drop table if exists t_meeting_notification;
drop table if exists t_meeting_attendee;
drop table if exists t_meeting_topic;
drop table if exists t_meeting_main;
drop table if exists t_meeting_authority;
drop table if exists t_wxdept_users;
drop table if exists t_wxdept;

create table t_wxdept (
    f_dept_id varchar(700) primary key,
    f_dept_nm varchar(500) not null,
    f_order_index integer,
    f_parent_dept_id varchar(700),
    f_create_time timestamp default now(),
    f_pathids varchar(2000),
    f_pathnames varchar(2000),
    f_tel varchar(100),
    f_deptnm_alias varchar(500)
);

create table t_wxdept_users (
    f_user_id varchar(500) primary key,
    f_user_name varchar(500) not null,
    f_dept_id varchar(700),
    f_phone varchar(100),
    f_create_time timestamp default now(),
    f_isleader varchar(10),
    f_position varchar(200),
    f_email varchar(200),
    f_gender varchar(10),
    f_avatar varchar(1000),
    f_english_name varchar(200),
    f_status varchar(20),
    f_qr_code varchar(1000),
    f_telephone varchar(100),
    f_isQianWei varchar(10),
    f_center_id varchar(100),
    f_xndept_id varchar(100),
    f_desc varchar(1000),
    f_emp_typ varchar(50),
    f_emp_status varchar(50),
    f_tb varchar(50),
    f_tags varchar(1000),
    f_enable varchar(10),
    needauth_user varchar(10),
    needauth_dept varchar(10),
    needauth_tag varchar(10)
);

create table t_meeting_authority (
    id bigserial primary key,
    user_id varchar(500) not null,
    user_name varchar(500) not null,
    dept_id varchar(700),
    authority smallint not null,
    group_code varchar(100) not null default 'DEFAULT',
    create_time timestamp not null default now(),
    unique(user_id, dept_id, group_code)
);

create table t_meeting_main (
    id bigserial primary key,
    meeting_date varchar(50) not null,
    meeting_time varchar(50) not null,
    period_no varchar(100),
    location varchar(500),
    leaders varchar(1000),
    content varchar(2000),
    status varchar(32) not null default 'DRAFT',
    creater varchar(500),
    created_time timestamp not null default now(),
    updated_time timestamp not null default now()
);

create table t_meeting_topic (
    id bigserial primary key,
    meeting_id bigint not null references t_meeting_main(id) on delete cascade,
    topic_type varchar(100),
    title varchar(1000) not null,
    report_dept_id varchar(2000),
    report_dept_name varchar(2000),
    participant_dept_id varchar(2000),
    participant_dept_name varchar(2000),
    summary text,
    sort_no integer not null default 1,
    status varchar(32) not null default 'WAITING',
    start_time timestamp,
    end_time timestamp,
    actual_minutes integer,
    conclusion text,
    notice varchar(2000),
    project_code varchar(200),
    report_dept_signed smallint not null default 0,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now()
);

create table t_meeting_attendee (
    id bigserial primary key,
    meeting_id bigint not null references t_meeting_main(id) on delete cascade,
    topic_id bigint not null references t_meeting_topic(id) on delete cascade,
    user_id varchar(500) not null,
    user_name varchar(500) not null,
    attendee_type varchar(32) not null,
    selected_by varchar(500),
    selected_source varchar(32) not null,
    selected_dept_id varchar(500),
    created_at timestamp not null default now(),
    confirm_status varchar(32) not null default 'PENDING',
    confirmed_by varchar(500),
    confirmed_at timestamp,
    sign_status varchar(32) not null default 'UNSIGNED',
    signed_at timestamp,
    unique(topic_id, user_id, attendee_type)
);

create table t_meeting_notification (
    id bigserial primary key,
    meeting_id bigint,
    topic_id bigint,
    title varchar(500) not null,
    msg_body text,
    tousers text,
    send_time timestamp,
    job_id varchar(200),
    created_at timestamp not null default now()
);

create table t_meeting_uploaded_file (
    id bigserial primary key,
    meeting_id bigint,
    file_name varchar(500),
    file_size bigint,
    parser_status varchar(32),
    parser_message varchar(1000),
    created_at timestamp not null default now()
);

create index idx_wx_user_dept on t_wxdept_users(f_dept_id);
create index idx_authority_group_user on t_meeting_authority(group_code, user_id);
create index idx_authority_dept_group on t_meeting_authority(dept_id, group_code);
create index idx_meeting_status on t_meeting_main(status);
create index idx_meeting_date on t_meeting_main(meeting_date, id);
create index idx_meeting_topic_meeting on t_meeting_topic(meeting_id, sort_no);
create index idx_topic_report_dept on t_meeting_topic(report_dept_id);
create index idx_topic_status on t_meeting_topic(meeting_id, status);
create index idx_attendee_meeting on t_meeting_attendee(meeting_id);
create index idx_attendee_user on t_meeting_attendee(user_id, sign_status);
create index idx_attendee_confirm on t_meeting_attendee(user_id, confirm_status);
create index idx_attendee_selected on t_meeting_attendee(meeting_id, selected_by, confirm_status);
create index idx_notice_meeting on t_meeting_notification(meeting_id);
create index idx_notice_topic on t_meeting_notification(topic_id);
create index idx_upload_meeting on t_meeting_uploaded_file(meeting_id);

comment on table t_wxdept is 'WeCom department table';
comment on table t_wxdept_users is 'WeCom user table';
comment on table t_meeting_authority is 'Meeting role authority table';
comment on table t_meeting_main is 'Meeting master table';
comment on table t_meeting_topic is 'Meeting topic table';
comment on table t_meeting_attendee is 'Meeting attendee and sign-in table';
comment on table t_meeting_notification is 'Meeting notification table';
comment on table t_meeting_uploaded_file is 'Topic import upload record table';

insert into t_wxdept(f_dept_id, f_dept_nm, f_order_index, f_parent_dept_id, f_pathids, f_pathnames) values
('D001', 'Office', 1, null, 'D001', 'Office'),
('D002', 'Planning Dept', 2, null, 'D002', 'Planning Dept'),
('D003', 'Data Center', 3, null, 'D003', 'Data Center'),
('D004', 'Security Dept', 4, null, 'D004', 'Security Dept'),
('D005', 'Network Ops', 5, null, 'D005', 'Network Ops');

insert into t_wxdept_users(f_user_id, f_user_name, f_dept_id, f_phone, f_position, f_email, f_enable) values
('U0001', 'Admin User', 'D001', '13800000001', 'Meeting Admin', 'admin@example.com', '1'),
('U0002', 'Planning Secretary', 'D002', '13800000002', 'Department Secretary', 'secretary-plan@example.com', '1'),
('U0003', 'Data Secretary', 'D003', '13800000003', 'Department Secretary', 'secretary-data@example.com', '1'),
('U0004', 'Planning Leader', 'D002', '13800000004', 'Team Leader', 'leader-plan@example.com', '1'),
('U0005', 'Data Leader', 'D003', '13800000005', 'Team Leader', 'leader-data@example.com', '1'),
('U0006', 'Planning Participant', 'D002', '13800000006', 'Engineer', 'plan-user@example.com', '1'),
('U0007', 'Data Participant', 'D003', '13800000007', 'Engineer', 'data-user@example.com', '1'),
('U0008', 'Security Secretary', 'D004', '13800000008', 'Department Secretary', 'secretary-safe@example.com', '1'),
('U0009', 'Network Participant', 'D005', '13800000009', 'Engineer', 'network-user@example.com', '1');

insert into t_meeting_authority(user_id, user_name, dept_id, authority, group_code) values
('U0001', 'Admin User', 'D001', 1, 'DEFAULT'),
('U0002', 'Planning Secretary', 'D002', 3, 'DEFAULT'),
('U0003', 'Data Secretary', 'D003', 3, 'DEFAULT'),
('U0004', 'Planning Leader', 'D002', 2, 'DEFAULT'),
('U0005', 'Data Leader', 'D003', 2, 'DEFAULT'),
('U0008', 'Security Secretary', 'D004', 3, 'DEFAULT');
