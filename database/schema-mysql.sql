drop table if exists t_meeting_uploaded_file;
drop table if exists t_meeting_notification;
drop table if exists t_meeting_attendee;
drop table if exists t_meeting_topic;
drop table if exists t_meeting_main;
drop table if exists t_meeting_authority;
drop table if exists t_wxdept_users;
drop table if exists t_wxdept;

create table t_wxdept (
    f_dept_id varchar(700) primary key comment 'WeCom department id',
    f_dept_nm varchar(500) not null comment 'Department name',
    f_order_index int comment 'Sort order',
    f_parent_dept_id varchar(700) comment 'Parent department id',
    f_create_time datetime default current_timestamp comment 'Create time',
    f_pathids varchar(2000) comment 'Department path ids',
    f_pathnames varchar(2000) comment 'Department path names',
    f_tel varchar(100) comment 'Department telephone',
    f_deptnm_alias varchar(500) comment 'Department alias'
) engine=InnoDB default charset=utf8mb4 comment='WeCom department table';

create table t_wxdept_users (
    f_user_id varchar(500) primary key comment 'WeCom user id; also login id and employee number',
    f_user_name varchar(500) not null comment 'User name',
    f_dept_id varchar(700) comment 'Department id',
    f_phone varchar(100) comment 'Mobile phone',
    f_create_time datetime default current_timestamp comment 'Create time',
    f_isleader varchar(10) comment 'WeCom department leader flag',
    f_position varchar(200) comment 'Position',
    f_email varchar(200) comment 'Email',
    f_gender varchar(10) comment 'Gender',
    f_avatar varchar(1000) comment 'Avatar url',
    f_english_name varchar(200) comment 'English name',
    f_status varchar(20) comment 'WeCom status',
    f_qr_code varchar(1000) comment 'WeCom QR code',
    f_telephone varchar(100) comment 'Office telephone',
    f_isQianWei varchar(10),
    f_center_id varchar(100),
    f_xndept_id varchar(100),
    f_desc varchar(1000),
    f_emp_typ varchar(50),
    f_emp_status varchar(50),
    f_tb varchar(50),
    f_tags varchar(1000),
    f_enable varchar(10) comment 'Enabled flag; 0 means disabled',
    needauth_user varchar(10),
    needauth_dept varchar(10),
    needauth_tag varchar(10),
    key idx_wx_user_dept(f_dept_id)
) engine=InnoDB default charset=utf8mb4 comment='WeCom user table';

create table t_meeting_authority (
    id bigint primary key auto_increment comment 'Primary key',
    user_id varchar(500) not null comment 'User id, references t_wxdept_users.f_user_id',
    user_name varchar(500) not null comment 'User name snapshot',
    dept_id varchar(700) comment 'Department id for this authority',
    authority tinyint not null comment '1 ADMIN, 2 LEADER, 3 SECRETARY',
    group_code varchar(100) not null default 'DEFAULT' comment 'Authority group code',
    create_time datetime not null default current_timestamp comment 'Create time',
    unique key uk_meeting_authority_user_dept_group(user_id, dept_id, group_code),
    key idx_authority_group_user(group_code, user_id),
    key idx_authority_dept_group(dept_id, group_code)
) engine=InnoDB default charset=utf8mb4 comment='Meeting role authority table';

create table t_meeting_main (
    id bigint primary key auto_increment comment 'Meeting id',
    meeting_date varchar(50) not null comment 'Meeting date, yyyy-MM-dd',
    meeting_time varchar(50) not null comment 'Meeting time, HH:mm',
    period_no varchar(100) comment 'Meeting period number',
    location varchar(500) comment 'Meeting location',
    leaders varchar(1000) comment 'Meeting leader display text',
    content varchar(2000) comment 'Meeting content summary',
    status varchar(32) not null default 'DRAFT' comment 'DRAFT/PUBLISHED/IN_PROGRESS/FINISHED',
    creater varchar(500) comment 'Creator user id',
    created_time datetime not null default current_timestamp comment 'Create time',
    updated_time datetime not null default current_timestamp on update current_timestamp comment 'Update time',
    key idx_meeting_status(status),
    key idx_meeting_date(meeting_date, id)
) engine=InnoDB default charset=utf8mb4 comment='Meeting master table';

create table t_meeting_topic (
    id bigint primary key auto_increment comment 'Topic id',
    meeting_id bigint not null comment 'Meeting id',
    topic_type varchar(100) comment 'Topic type',
    title varchar(1000) not null comment 'Topic title',
    report_dept_id varchar(2000) comment 'Report department ids, comma separated',
    report_dept_name varchar(2000) comment 'Report department names, comma separated',
    participant_dept_id varchar(2000) comment 'Participant department ids, comma separated',
    participant_dept_name varchar(2000) comment 'Participant department names, comma separated',
    summary text comment 'Meeting minutes',
    sort_no int not null default 1 comment 'Topic sort number',
    status varchar(32) not null default 'WAITING' comment 'WAITING/RUNNING/FINISHED',
    start_time datetime comment 'Actual start time',
    end_time datetime comment 'Actual end time',
    actual_minutes int comment 'Actual duration in minutes',
    conclusion text comment 'Meeting conclusion',
    notice varchar(2000) comment 'Parsed topic notice title',
    project_code varchar(200) comment 'Parsed project code',
    report_dept_signed tinyint not null default 0 comment 'Whether report department signed, 0/1',
    created_at datetime not null default current_timestamp comment 'Create time',
    updated_at datetime not null default current_timestamp on update current_timestamp comment 'Update time',
    constraint fk_topic_meeting foreign key(meeting_id) references t_meeting_main(id) on delete cascade,
    key idx_meeting_topic_meeting(meeting_id, sort_no),
    key idx_topic_report_dept(report_dept_id),
    key idx_topic_status(meeting_id, status)
) engine=InnoDB default charset=utf8mb4 comment='Meeting topic table';

create table t_meeting_attendee (
    id bigint primary key auto_increment comment 'Primary key',
    meeting_id bigint not null comment 'Meeting id',
    topic_id bigint not null comment 'Topic id',
    user_id varchar(500) not null comment 'Attendee user id',
    user_name varchar(500) not null comment 'Attendee user name snapshot',
    attendee_type varchar(32) not null comment 'REPORT/PARTAKE',
    selected_by varchar(500) comment 'Selector user id',
    selected_source varchar(32) not null comment 'ADMIN/DEPARTMENT',
    selected_dept_id varchar(500) comment 'Selecting department id',
    created_at datetime not null default current_timestamp comment 'Create time',
    confirm_status varchar(32) not null default 'PENDING' comment 'PENDING/CONFIRMED',
    confirmed_by varchar(500) comment 'Confirmer user id',
    confirmed_at datetime comment 'Confirm time',
    sign_status varchar(32) not null default 'UNSIGNED' comment 'UNSIGNED/SIGNED',
    signed_at datetime comment 'Sign time',
    unique key uk_attendee_topic_user_type(topic_id, user_id, attendee_type),
    key idx_attendee_meeting(meeting_id),
    key idx_attendee_user(user_id, sign_status),
    key idx_attendee_confirm(user_id, confirm_status),
    key idx_attendee_selected(meeting_id, selected_by, confirm_status),
    constraint fk_attendee_meeting foreign key(meeting_id) references t_meeting_main(id) on delete cascade,
    constraint fk_attendee_topic foreign key(topic_id) references t_meeting_topic(id) on delete cascade
) engine=InnoDB default charset=utf8mb4 comment='Meeting attendee and sign-in table';

create table t_meeting_notification (
    id bigint primary key auto_increment comment 'Notification id',
    meeting_id bigint comment 'Meeting id',
    topic_id bigint comment 'Topic id',
    title varchar(500) not null comment 'Notification title',
    msg_body text comment 'Notification body',
    tousers text comment 'Receiver user ids',
    send_time datetime comment 'Send time',
    job_id varchar(200) comment 'External job id or mock job id',
    created_at datetime not null default current_timestamp comment 'Create time',
    key idx_notice_meeting(meeting_id),
    key idx_notice_topic(topic_id)
) engine=InnoDB default charset=utf8mb4 comment='Meeting notification table';

create table t_meeting_uploaded_file (
    id bigint primary key auto_increment comment 'Upload record id',
    meeting_id bigint comment 'Meeting id',
    file_name varchar(500) comment 'File name',
    file_size bigint comment 'File size in bytes',
    parser_status varchar(32) comment 'SUCCESS/FAILED',
    parser_message varchar(1000) comment 'Parser message',
    created_at datetime not null default current_timestamp comment 'Create time',
    key idx_upload_meeting(meeting_id)
) engine=InnoDB default charset=utf8mb4 comment='Topic import upload record table';

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
