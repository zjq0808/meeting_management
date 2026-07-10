alter table t_meeting_attendee
    add column confirm_status varchar(32) not null default 'PENDING' comment '确认状态：PENDING/CONFIRMED',
    add column confirmed_by varchar(500) null comment '确认人用户ID',
    add column confirmed_at datetime null comment '确认时间';

create index idx_attendee_confirm on t_meeting_attendee(user_id, confirm_status);
