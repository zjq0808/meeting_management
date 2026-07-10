insert into t_wxdept(f_dept_id, f_dept_nm, f_order_index, f_parent_dept_id, f_pathids, f_pathnames) values
('D001', '办公室', 1, null, 'D001', '办公室'),
('D002', '技术规划处', 2, null, 'D002', '技术规划处'),
('D003', '数据中心', 3, null, 'D003', '数据中心'),
('D004', '安全管理处', 4, null, 'D004', '安全管理处');

insert into t_wxdept_users(f_user_id, f_user_name, f_dept_id, f_phone, f_position, f_email, f_enable) values
('U0001', '会议管理员', 'D001', '13800000001', '会议管理员', 'admin@example.com', '1'),
('U0002', '规划处秘书', 'D002', '13800000002', '部门秘书', 'secretary-plan@example.com', '1'),
('U0003', '数据中心秘书', 'D003', '13800000003', '部门秘书', 'secretary-data@example.com', '1'),
('U0004', '规划科组长', 'D002', '13800000004', '科组长', 'leader-plan@example.com', '1'),
('U0005', '数据科组长', 'D003', '13800000005', '科组长', 'leader-data@example.com', '1'),
('U0006', '规划参会人', 'D002', '13800000006', '工程师', 'plan-user@example.com', '1'),
('U0007', '数据参会人', 'D003', '13800000007', '工程师', 'data-user@example.com', '1'),
('U0008', '安全处秘书', 'D004', '13800000008', '部门秘书', 'secretary-safe@example.com', '1');

insert into t_meeting_authority(user_id, user_name, dept_id, authority, group_code) values
('U0001', '会议管理员', 'D001', 1, 'DEFAULT'),
('U0002', '规划处秘书', 'D002', 3, 'DEFAULT'),
('U0003', '数据中心秘书', 'D003', 3, 'DEFAULT'),
('U0004', '规划科组长', 'D002', 2, 'DEFAULT'),
('U0005', '数据科组长', 'D003', 2, 'DEFAULT'),
('U0008', '安全处秘书', 'D004', 3, 'DEFAULT');
