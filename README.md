# 三重一大会议管理系统

本项目包含 Spring Boot 后端、Vue 前端和数据库初始化脚本。当前后端已迁移到新的企业微信表体系：用户和部门来自 `t_wxdept_users`、`t_wxdept`，会议权限来自 `t_meeting_authority`，会议业务数据写入 `t_meeting_main`、`t_meeting_topic`、`t_meeting_attendee`、`t_meeting_notification`、`t_meeting_uploaded_file`。

## 项目目录

- `backend/`：Java 8 + Spring Boot 2.7 + MyBatis + Jetty 后端接口服务。
- `frontend/`：Vue 3 + Vue Router 4 + Vuex 4 前端工程。
- `database/schema-mysql.sql`：MySQL 本地初始化脚本。
- `database/schema.sql`：Kingbase/PostgreSQL 兼容初始化脚本。
- `docs/api.md`：Postman 接口调试文档。

## 本地启动

### 1. 初始化 MySQL

```sql
create database meeting_management default character set utf8mb4 collate utf8mb4_unicode_ci;
use meeting_management;
source C:/workspace/meeting_management/database/schema-mysql.sql;
```

默认后端数据源配置在 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://${MYSQL_HOST:127.0.0.1}:${MYSQL_PORT:3306}/${MYSQL_DATABASE:meeting_management}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: ${MYSQL_USERNAME:root}
    password: ${MYSQL_PASSWORD:123456}
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端默认地址：`http://localhost:9090`。

### 3. 启动前端

```bash
cd frontend
npm install
npm run serve
```

前端默认地址：`http://localhost:8082/`。前端通过 `/api` 代理到 `http://localhost:9090`。

## 演示账号

登录接口读取 `t_wxdept_users`，优先使用 `userId` 匹配 `f_user_id`，兼容 `username` 精确匹配 `f_user_name`。当前本地实现不校验密码。

| userId | 姓名 | 部门 | 角色来源 |
| --- | --- | --- | --- |
| `U0001` | 会议管理员 | 办公室 | `t_meeting_authority.authority=1` |
| `U0002` | 规划处秘书 | 技术规划处 | `t_meeting_authority.authority=3` |
| `U0003` | 数据中心秘书 | 数据中心 | `t_meeting_authority.authority=3` |
| `U0004` | 规划科组长 | 技术规划处 | `t_meeting_authority.authority=2` |
| `U0005` | 数据科组长 | 数据中心 | `t_meeting_authority.authority=2` |
| `U0006` | 规划参会人 | 技术规划处 | 无权限记录，默认为 `PARTICIPANT` |
| `U0007` | 数据参会人 | 数据中心 | 无权限记录，默认为 `PARTICIPANT` |
| `U0008` | 安全处秘书 | 安全管理处 | `t_meeting_authority.authority=3` |

登录示例：

```http
POST http://localhost:9090/api/auth/login
Content-Type: application/json

{"userId":"U0001"}
```

后续接口请求头：

```http
Authorization: Bearer <登录返回token>
```

## 权限与状态

### 角色映射

| `t_meeting_authority.authority` | 接口返回 `role` | 说明 |
| --- | --- | --- |
| `1` | `ADMIN` | 会议管理员，可创建、编辑、发布、控制会议。 |
| `2` | `LEADER` | 科组长/部门领导，可处理本人相关遴选任务。 |
| `3` | `SECRETARY` | 部门秘书，可处理本部门相关议题遴选任务。 |
| 无记录 | `PARTICIPANT` | 普通参会人员。 |

权限查询会按 `meeting.group-code` 过滤 `group_code`，默认值为 `DEFAULT`。

### 会议状态

| 状态 | 说明 |
| --- | --- |
| `DRAFT` | 草稿，创建后默认状态。 |
| `PUBLISHED` | 已发布，待开始。 |
| `IN_PROGRESS` | 会议进行中，至少一个议题已开始。 |
| `FINISHED` | 全部议题结束后会议完成。 |

### 议题状态

| 状态 | 说明 |
| --- | --- |
| `WAITING` | 待讨论，议题创建后默认状态。 |
| `RUNNING` | 讨论中，同一会议只允许一个议题处于该状态。 |
| `FINISHED` | 已结束，记录结束时间和实际时长。 |

### 参会类型

| 类型 | 说明 |
| --- | --- |
| `LEADER` | 参会领导/科组长。 |
| `SHARE` | 分享/抄送人员预留类型。 |
| `REPORT` | 汇报人。 |
| `PARTAKE` | 普通参会人。 |
| `PARTICIPANT` | 兼容旧入参，后端会转换成 `PARTAKE`。 |

## 数据库表字段说明

### `t_wxdept` 企业微信部门表

用途：保存企业微信部门组织结构，供用户归属、议题汇报部门、议题参会部门和权限部门使用。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `f_dept_id` | `varchar(700)` | 部门 ID，主键，来自企业微信部门标识。 |
| `f_dept_nm` | `varchar(500)` | 部门名称。 |
| `f_order_index` | `int` | 部门排序号，用于通讯录或选择器展示。 |
| `f_parent_dept_id` | `varchar(700)` | 上级部门 ID，根部门可为空。 |
| `f_create_time` | `datetime` | 部门数据创建或同步时间。 |
| `f_pathids` | `varchar(2000)` | 部门路径 ID，用于保存层级链路。 |
| `f_pathnames` | `varchar(2000)` | 部门路径名称，用于展示完整层级。 |
| `f_tel` | `varchar(100)` | 部门联系电话。 |
| `f_deptnm_alias` | `varchar(500)` | 部门别名或简称。 |

### `t_wxdept_users` 企业微信用户表

用途：保存企业微信用户。当前系统使用 `f_user_id` 作为登录 ID、员工编码和业务用户 ID。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `f_user_id` | `varchar(500)` | 用户 ID，主键；接口返回为 `id/userId/employeeNo`。 |
| `f_user_name` | `varchar(500)` | 用户姓名；接口返回为 `username/realName`。 |
| `f_dept_id` | `varchar(700)` | 所属部门 ID，关联 `t_wxdept.f_dept_id`。 |
| `f_phone` | `varchar(100)` | 手机号，短信或通讯录展示预留。 |
| `f_create_time` | `datetime` | 用户数据创建或同步时间。 |
| `f_isleader` | `varchar(10)` | 企业微信部门领导标识。 |
| `f_position` | `varchar(200)` | 职务/岗位。 |
| `f_email` | `varchar(200)` | 邮箱。 |
| `f_gender` | `varchar(10)` | 性别。 |
| `f_avatar` | `varchar(1000)` | 头像 URL。 |
| `f_english_name` | `varchar(200)` | 英文名。 |
| `f_status` | `varchar(20)` | 企业微信用户状态。 |
| `f_qr_code` | `varchar(1000)` | 个人二维码。 |
| `f_telephone` | `varchar(100)` | 座机号码。 |
| `f_isQianWei` | `varchar(10)` | 企业微信同步预留字段。 |
| `f_center_id` | `varchar(100)` | 中心 ID 预留字段。 |
| `f_xndept_id` | `varchar(100)` | 虚拟/扩展部门 ID 预留字段。 |
| `f_desc` | `varchar(1000)` | 用户描述。 |
| `f_emp_typ` | `varchar(50)` | 员工类型。 |
| `f_emp_status` | `varchar(50)` | 员工状态。 |
| `f_tb` | `varchar(50)` | 同步标识预留字段。 |
| `f_tags` | `varchar(1000)` | 用户标签。 |
| `f_enable` | `varchar(10)` | 是否启用；当前查询中 `0` 表示停用。 |
| `needauth_user` | `varchar(10)` | 企业微信授权同步预留字段。 |
| `needauth_dept` | `varchar(10)` | 企业微信部门授权预留字段。 |
| `needauth_tag` | `varchar(10)` | 企业微信标签授权预留字段。 |

### `t_meeting_authority` 会议权限表

用途：配置会议系统角色。程序通过该表判断用户是否为会议管理员、科组长或部门秘书。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 主键，自增。 |
| `user_id` | `varchar(500)` | 用户 ID，关联 `t_wxdept_users.f_user_id`。 |
| `user_name` | `varchar(500)` | 用户姓名，便于人工排查。 |
| `dept_id` | `varchar(700)` | 权限所属部门 ID。秘书/科组长按该部门处理任务。 |
| `authority` | `tinyint/smallint` | 权限值：`1` 管理员，`2` 科组长/部门领导，`3` 部门秘书。 |
| `group_code` | `varchar(100)` | 权限分组，默认 `DEFAULT`；程序按配置项 `meeting.group-code` 过滤。 |
| `create_time` | `datetime` | 权限记录创建时间。 |

唯一约束：`user_id + dept_id + group_code`。

### `t_meeting_main` 会议主表

用途：保存会议基础信息和会议流转状态。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 会议 ID，主键，自增。 |
| `meeting_date` | `varchar(50)` | 会议日期，接口字段为 `meetingDate`。 |
| `meeting_time` | `varchar(50)` | 会议时间，接口字段为 `meetingTime`。 |
| `period_no` | `varchar(100)` | 会议期次，接口字段为 `periodNo`。 |
| `location` | `varchar(500)` | 会议地点。 |
| `content` | `varchar(2000)` | 会议内容或会议说明。 |
| `status` | `varchar(32)` | 会议状态：`DRAFT/PUBLISHED/IN_PROGRESS/FINISHED`。 |
| `creater` | `varchar(500)` | 创建人用户 ID，对应当前登录用户 `f_user_id`。 |
| `created_time` | `datetime` | 创建时间。 |
| `updated_time` | `datetime` | 更新时间。 |

### `t_meeting_topic` 会议议题表

用途：保存会议议题、汇报部门、参会部门、议题进度、实际时长、会议结论和汇报部门签到状态。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 议题 ID，主键，自增。 |
| `meeting_id` | `bigint` | 所属会议 ID，关联 `t_meeting_main.id`。 |
| `topic_type` | `varchar(100)` | 议题类型，接口字段为 `topicType`。 |
| `title` | `varchar(1000)` | 议题标题。 |
| `report_dept_id` | `varchar(700)` | 汇报部门 ID，接口字段为 `reportDepartmentId`。 |
| `report_dept_name` | `varchar(500)` | 汇报部门名称，接口字段为 `reportDepartmentName`。 |
| `participant_dept_id` | `varchar(2000)` | 参会部门 ID，多个部门使用英文逗号分隔。 |
| `participant_dept_name` | `varchar(2000)` | 参会部门名称，多个部门使用英文逗号分隔。 |
| `summary` | `text` | 议题简述。 |
| `sort_no` | `int` | 议题排序号，接口字段为 `sortNo`。 |
| `status` | `varchar(32)` | 议题状态：`WAITING/RUNNING/FINISHED`。 |
| `start_time` | `datetime` | 议题开始时间。 |
| `end_time` | `datetime` | 议题结束时间。 |
| `actual_minutes` | `int` | 实际讨论时长，单位分钟；可由后端计算或手动传入。 |
| `conclusion` | `text` | 议题结论。 |
| `report_dept_signed` | `tinyint/smallint` | 汇报部门签到状态：`0` 未签到，`1` 已签到。 |
| `created_at` | `datetime` | 创建时间。 |
| `updated_at` | `datetime` | 更新时间。 |

### `t_meeting_attendee` 会议参会与签到表

用途：统一保存每个议题的参会人员、人员类型、提交人和签到状态。当前不再使用独立签到表。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 主键，自增。 |
| `meeting_id` | `bigint` | 会议 ID，关联 `t_meeting_main.id`。 |
| `topic_id` | `bigint` | 议题 ID，关联 `t_meeting_topic.id`。 |
| `user_id` | `varchar(500)` | 参会用户 ID，对应 `t_wxdept_users.f_user_id`。 |
| `user_name` | `varchar(500)` | 参会用户姓名。 |
| `attendee_type` | `varchar(32)` | 参会类型：`LEADER/SHARE/REPORT/PARTAKE`。旧入参 `PARTICIPANT` 会转换为 `PARTAKE`。 |
| `selected_by` | `varchar(500)` | 名单提交人用户 ID。重复提交时按 `topic_id + attendee_type + selected_by` 删除旧名单再插入新名单。 |
| `created_at` | `datetime` | 创建时间。 |
| `sign_status` | `varchar(32)` | 签到状态：`UNSIGNED/SIGNED`。 |
| `signed_at` | `datetime` | 签到时间。 |

唯一约束：`topic_id + user_id + attendee_type`。

### `t_meeting_notification` 会议通知表

用途：记录站内通知和后续企业微信/短信发送任务。本地默认只落库并打印日志，不真实发送。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 通知 ID，主键，自增。 |
| `meeting_id` | `bigint` | 关联会议 ID。 |
| `topic_id` | `bigint` | 关联议题 ID，可为空。 |
| `title` | `varchar(500)` | 通知标题。 |
| `msg_body` | `text` | 通知正文。 |
| `tousers` | `text` | 接收人用户 ID，多个用户可用英文逗号分隔。 |
| `send_time` | `datetime` | 发送时间。 |
| `job_id` | `varchar(200)` | 通知任务 ID。 |
| `created_at` | `datetime` | 创建时间。 |

### `t_meeting_uploaded_file` 议题上传文件表

用途：记录 Word 议题文件导入解析结果。当前导入接口只解析返回 JSON，不直接保存到 `t_meeting_topic`。

| 字段名 | 类型 | 注释 |
| --- | --- | --- |
| `id` | `bigint` | 上传记录 ID，主键，自增。 |
| `meeting_id` | `bigint` | 所属会议 ID。 |
| `file_name` | `varchar(500)` | 上传文件名。 |
| `file_size` | `bigint` | 文件大小，单位字节。 |
| `parser_status` | `varchar(32)` | 解析状态：`SUCCESS/FAILED`。 |
| `parser_message` | `varchar(1000)` | 解析结果说明或失败原因。 |
| `created_at` | `datetime` | 创建时间。 |

## 主要接口

详见 [docs/api.md](docs/api.md)。当前后端暴露接口：

- `POST /api/auth/login`
- `GET /api/auth/me`
- `GET /api/departments`
- `GET /api/users`
- `GET /api/meetings`
- `POST /api/meetings`
- `PUT /api/meetings/{id}`
- `GET /api/meetings/{id}`
- `GET /api/meetings/{id}/detail`
- `POST /api/meetings/{id}/publish`
- `POST /api/meetings/{id}/topics/import`
- `GET /api/meetings/{id}/selection-tasks`
- `POST /api/topics/{topicId}/attendees`
- `POST /api/sign-in`
- `POST /api/topics/{topicId}/start`
- `POST /api/topics/{topicId}/end`
- `PUT /api/topics/{topicId}/conclusion`
- `GET /api/meetings/{id}/dashboard`
- `GET /api/meetings/{id}/signin-dashboard`

## 验证

```bash
cd backend
mvn test
```

当前测试覆盖：企微用户登录、权限映射、管理员权限、会议列表过滤、创建/发布、导入只解析、参会遴选、签到、议题计时、结论保存和签到看板统计。
