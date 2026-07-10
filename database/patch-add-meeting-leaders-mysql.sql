alter table t_meeting_main
    add column leaders varchar(1000) null comment '参会领导' after location;
