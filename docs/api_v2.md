# 三重一大会议管理系统接口文档 v2

本文档按当前后端代码整理，适用于本地 Postman 调试。接口统一返回：

```json
{
  "success": true,
  "message": "OK",
  "data": {}
}
```

业务异常返回：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

## 1. 基础约定

- 本地服务地址：`http://localhost:9090`
- Base URL：`{{baseUrl}} = http://localhost:9090/api`
- 除登录接口外，其余接口需要请求头：

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

- 登录使用企业微信用户 ID，即 `t_wxdept_users.f_user_id`。
- 当前演示用户：

| userId | 姓名 | 角色 | 部门 |
| --- | --- | --- | --- |
| U0001 | Admin User | ADMIN 会议管理员 | Office |
| U0002 | Planning Secretary | SECRETARY 部门秘书 | Planning Dept |
| U0003 | Data Secretary | SECRETARY 部门秘书 | Data Center |
| U0004 | Planning Leader | LEADER 科组长 | Planning Dept |
| U0005 | Data Leader | LEADER 科组长 | Data Center |
| U0006 | Planning Participant | PARTICIPANT 普通参会人 | Planning Dept |
| U0007 | Data Participant | PARTICIPANT 普通参会人 | Data Center |
| U0008 | Security Secretary | SECRETARY 部门秘书 | Security Dept |

## 2. 权限和状态规则

### 2.1 用户角色

用户角色来自 `t_meeting_authority.authority`：

| authority | role | 说明 |
| --- | --- | --- |
| 1 | ADMIN | 会议管理员，可创建、编辑、发布会议，可查看全部会议 |
| 2 | LEADER | 科组长，被秘书确认后可看到相关会议，可继续选择普通参会人员 |
| 3 | SECRETARY | 部门秘书，会议发布后可看到本部门作为汇报部门或参会部门的会议 |
| 无记录 | PARTICIPANT | 普通参会人，被确认后才能看到会议并签到 |

### 2.2 会议状态

| 状态 | 说明 |
| --- | --- |
| DRAFT | 草稿，只有会议管理员可见 |
| PUBLISHED | 已发布，秘书可开始遴选人员 |
| IN_PROGRESS | 会议进行中 |
| FINISHED | 会议已结束 |

### 2.3 议题状态

| 状态 | 说明 |
| --- | --- |
| WAITING | 待讨论 |
| RUNNING | 正在讨论 |
| FINISHED | 已结束 |

### 2.4 参会确认和签到状态

`t_meeting_attendee.confirm_status`：

| 状态 | 说明 |
| --- | --- |
| PENDING | 已被秘书或科组长选择，但尚未确认，不出现在本人会议列表 |
| CONFIRMED | 已确认参会，会出现在本人会议列表，可扫码签到 |

`t_meeting_attendee.sign_status`：

| 状态 | 说明 |
| --- | --- |
| UNSIGNED | 未签到 |
| SIGNED | 已签到 |

## 3. 认证接口

### 3.1 登录

```http
POST {{baseUrl}}/auth/login
Content-Type: application/json
```

请求体：

```json
{
  "userId": "U0001"
}
```

兼容按姓名精确登录：

```json
{
  "username": "Admin User"
}
```

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "token": "本地内存token",
    "user": {
      "id": "U0001",
      "userId": "U0001",
      "employeeNo": "U0001",
      "username": "Admin User",
      "realName": "Admin User",
      "departmentId": "D001",
      "departmentName": "Office",
      "role": "ADMIN",
      "mobile": "13800000001"
    }
  }
}
```

### 3.2 查询当前登录人

```http
GET {{baseUrl}}/auth/me
Authorization: Bearer {{token}}
```

响应 `data` 为当前用户对象。

## 4. 通讯录接口

### 4.1 查询部门

```http
GET {{baseUrl}}/departments
Authorization: Bearer {{token}}
```

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "D002",
      "deptId": "D002",
      "name": "Planning Dept",
      "parentId": null,
      "sortNo": 2
    }
  ]
}
```

### 4.2 查询用户

```http
GET {{baseUrl}}/users?departmentId=D002&role=LEADER&keyword=Planning
Authorization: Bearer {{token}}
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| departmentId | 否 | 按部门过滤 |
| role | 否 | `ADMIN`、`LEADER`、`SECRETARY`、`PARTICIPANT` |
| keyword | 否 | 按姓名或 userId 模糊搜索 |

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": "U0004",
      "userId": "U0004",
      "employeeNo": "U0004",
      "username": "Planning Leader",
      "realName": "Planning Leader",
      "departmentId": "D002",
      "departmentName": "Planning Dept",
      "role": "LEADER",
      "mobile": "13800000004"
    }
  ]
}
```

## 5. 会议接口

### 5.1 查询会议列表

```http
GET {{baseUrl}}/meetings
Authorization: Bearer {{token}}
```

规则：

- `ADMIN` 查看全部会议，包含草稿。
- `SECRETARY` 只查看已发布且本部门是汇报部门或参会部门的会议。
- `LEADER`、`PARTICIPANT` 只查看自己已确认参会的会议。
- 被选择但未点击“确认参会人员”的人员，列表中看不到会议。

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": [
    {
      "id": 1,
      "meetingDate": "2026-07-11",
      "meetingTime": "15:00",
      "periodNo": "2026年第4期",
      "location": "会议室A",
      "leaders": "局领导",
      "content": "三重一大事项审议",
      "status": "PUBLISHED",
      "createdBy": "U0001",
      "topicCount": 2
    }
  ]
}
```

### 5.2 创建会议

```http
POST {{baseUrl}}/meetings
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

仅 `ADMIN` 可调用。

请求体：

```json
{
  "meetingDate": "2026-07-11",
  "meetingTime": "15:00",
  "periodNo": "2026年第4期",
  "location": "会议室A",
  "leaders": "局领导",
  "content": "三重一大事项审议",
  "topics": [
    {
      "topicType": "三重一大",
      "title": "技术平台升级事项",
      "reportDepartmentId": "D002",
      "participantDepartmentIds": ["D002", "D003"],
      "summary": "讨论技术平台升级方案",
      "sortNo": 1
    }
  ]
}
```

说明：

- `reportDepartmentName` 可不传，后端按部门 ID 自动补充。
- `participantDepartments` 可不传，后端按部门 ID 自动补充。
- `participantDepartmentIds` 保存到 `t_meeting_topic.participant_dept_id`，多个部门以逗号存储。

响应 `data` 为会议详情聚合对象。

### 5.3 编辑会议

```http
PUT {{baseUrl}}/meetings/{{meetingId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

请求体同创建会议。传入 `topics[].id` 表示更新原议题；未传 `id` 表示新增议题；已存在但本次未提交的议题会被删除。

### 5.4 查询会议详情

```http
GET {{baseUrl}}/meetings/{{meetingId}}
Authorization: Bearer {{token}}
```

兼容路径：

```http
GET {{baseUrl}}/meetings/{{meetingId}}/detail
```

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "id": 1,
    "meetingDate": "2026-07-11",
    "meetingTime": "15:00",
    "periodNo": "2026年第4期",
    "location": "会议室A",
    "leaders": "局领导",
    "content": "三重一大事项审议",
    "status": "PUBLISHED",
    "topics": [
      {
        "id": 1,
        "meetingId": 1,
        "topicType": "三重一大",
        "title": "技术平台升级事项",
        "reportDepartmentId": "D002",
        "reportDepartmentName": "Planning Dept",
        "participantDeptId": "D002,D003",
        "participantDepartmentIds": ["D002", "D003"],
        "participantDepartments": "Planning Dept,Data Center",
        "summary": "讨论技术平台升级方案",
        "sortNo": 1,
        "status": "WAITING",
        "startTime": null,
        "endTime": null,
        "actualMinutes": null,
        "conclusion": null,
        "reportDepartmentSigned": 0,
        "attendees": []
      }
    ],
    "attendees": [],
    "signIns": [],
    "topicSignStats": [
      {
        "id": 1,
        "sortNo": 1,
        "title": "技术平台升级事项",
        "reportDepartmentName": "Planning Dept",
        "signed": false
      }
    ]
  }
}
```

### 5.5 发布会议

```http
POST {{baseUrl}}/meetings/{{meetingId}}/publish
Authorization: Bearer {{adminToken}}
```

行为：

- 会议至少需要一个议题。
- 会议状态改为 `PUBLISHED`。
- 后端按议题汇报部门和参会部门查找部门秘书，写入 `t_meeting_notification`。
- 未找到秘书的部门会通知管理员。

### 5.6 导入议题 Word

```http
POST {{baseUrl}}/meetings/{{meetingId}}/topics/import
Authorization: Bearer {{adminToken}}
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| file | File | `.doc` 或 `.docx`，最大 50MB |

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "meetingId": 1,
    "fileName": "topics.docx",
    "parserStatus": "SUCCESS",
    "parserMessage": "识别到 2 个议题",
    "topicCount": 2,
    "topics": [
      {
        "topicType": "三重一大",
        "title": "技术平台升级事项",
        "reportDepartmentId": "D002",
        "reportDepartmentName": "Planning Dept",
        "participantDepartmentIds": ["D002", "D003"],
        "participantDepartments": "Planning Dept,Data Center",
        "summary": "讨论技术平台升级方案",
        "sortNo": 1
      }
    ]
  }
}
```

注意：当前实现“只解析返回”，不会直接写入 `t_meeting_topic`。前端需要把返回的 `topics` 放入会议编辑表单，再调用创建或编辑会议接口保存。

## 6. 参会遴选接口

### 6.1 查询当前用户可处理的议题

```http
GET {{baseUrl}}/meetings/{{meetingId}}/selection-tasks
Authorization: Bearer {{token}}
```

规则：

- `SECRETARY` 返回本部门作为汇报部门或参会部门的议题。
- 其他角色返回全部议题，但只有管理员和有权限的秘书可以提交人员。

### 6.2 提交议题参会人员

```http
POST {{baseUrl}}/topics/{{topicId}}/attendees
Authorization: Bearer {{token}}
Content-Type: application/json
```

请求体：

```json
{
  "attendeeType": "REPORT",
  "userIds": ["U0004"]
}
```

`attendeeType`：

| 值 | 说明 |
| --- | --- |
| REPORT | 汇报人 |
| PARTAKE | 参会人 |

规则：

- 仅接受 `REPORT`、`PARTAKE`，`userIds` 可为空以清空有权维护的名单。
- 管理员可维护全部来源记录；管理员新增记录直接确认为 `CONFIRMED`。
- 秘书可维护本汇报部门的 `REPORT` 和本参会部门的 `PARTAKE`；新增记录为 `PENDING`。
- 管理员来源记录只能由管理员删除；部门来源记录可由本部门秘书或管理员删除。
- 仅 `PUBLISHED`、`IN_PROGRESS` 状态允许调整人员。
- 单议题最多 `meeting.max-attendees-per-topic` 人，默认 60。

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "topic": {},
    "attendees": [
      {
        "id": 1,
        "meeting_id": 1,
        "topic_id": 1,
        "user_id": "U0004",
        "username": "Planning Leader",
        "employee_no": "U0004",
        "attendee_type": "REPORT",
        "selected_source": "DEPARTMENT",
        "selected_dept_id": "D002",
        "confirm_status": "PENDING",
        "sign_status": "UNSIGNED"
      }
    ]
  }
}
```

### 6.3 确认参会人员

```http
POST {{baseUrl}}/meetings/{{meetingId}}/attendees/confirm
Authorization: Bearer {{secretaryToken}}
```

规则：

- 仅 `SECRETARY` 可调用。
- 会议必须已发布。
- 确认本部门来源且仍为 `PENDING` 的人员。
- 确认后：
  - `confirm_status` 改为 `CONFIRMED`
  - 写入 `confirmed_by`、`confirmed_at`
  - 写入通知表 `t_meeting_notification`
  - 被确认人员才能在会议列表看到会议并扫码签到

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "meetingId": 1,
    "confirmedCount": 1,
    "attendees": []
  }
}
```

## 7. 签到接口

### 7.1 生成会议签到二维码 token

```http
POST {{baseUrl}}/meetings/{{meetingId}}/sign-qrcode
Authorization: Bearer {{adminToken}}
```

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "meetingId": 1,
    "token": "b6b7d8e7...",
    "expiresAt": "2026-07-09T18:00:00",
    "url": "/mobile/sign-in?token=b6b7d8e7..."
  }
}
```

说明：

- token 保存在后端内存，服务重启后失效。
- 默认有效期由 `meeting.sign-token-ttl-minutes` 控制，当前默认 240 分钟。

### 7.2 扫码签到预览

扫码后，前端先调用该接口展示本人相关议题。

```http
GET {{baseUrl}}/sign-in/preview?token={{signToken}}
Authorization: Bearer {{token}}
```

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "meeting": {
      "id": 1,
      "meetingDate": "2026-07-11",
      "meetingTime": "15:00",
      "periodNo": "2026年第4期",
      "location": "会议室A",
      "status": "PUBLISHED"
    },
    "topics": [
      {
        "attendeeId": 10,
        "topicId": 1,
        "attendeeType": "PARTAKE",
        "signStatus": "UNSIGNED",
        "signed": false,
        "title": "技术平台升级事项",
        "sortNo": 1,
        "reportDepartmentName": "Planning Dept"
      }
    ],
    "canSign": true,
    "message": "",
    "expiresAt": "2026-07-09T18:00:00"
  }
}
```

### 7.3 确认签到

```http
POST {{baseUrl}}/sign-in
Authorization: Bearer {{token}}
Content-Type: application/json
```

请求体：

```json
{
  "token": "{{signToken}}"
}
```

规则：

- 当前登录人必须存在 `CONFIRMED` 且 `UNSIGNED` 的参会记录。
- 一次签到会把该用户在本会议下所有未签到且已确认的相关议题更新为 `SIGNED`。
- 如果当前用户所属部门是议题汇报部门，会同时更新 `t_meeting_topic.report_dept_signed=1`。
- 重复签到会返回业务异常。

响应：

```json
{
  "success": true,
  "message": "OK",
  "data": {
    "message": "签到成功",
    "topics": []
  }
}
```

## 8. 会议控制台接口

### 8.1 开始议题

```http
POST {{baseUrl}}/topics/{{topicId}}/start
Authorization: Bearer {{adminToken}}
```

行为：

- 会议状态改为 `IN_PROGRESS`。
- 当前议题状态改为 `RUNNING`。
- 写入 `start_time=now()`。
- 同一会议已有 `RUNNING` 议题时不允许开始下一个。

响应为会议 dashboard 聚合数据。

### 8.2 结束议题

```http
POST {{baseUrl}}/topics/{{topicId}}/end
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

请求体可为空，也可手动指定时长：

```json
{
  "actualMinutes": 12
}
```

行为：

- 当前议题必须是 `RUNNING`。
- 写入 `end_time=now()`。
- `actual_minutes` 优先使用手动值；未传则按 `start_time` 到当前时间计算，最小 1 分钟。
- 如果全部议题都已 `FINISHED`，会议状态改为 `FINISHED`。

### 8.3 更新议题信息

```http
PUT {{baseUrl}}/topics/{{topicId}}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

请求体：

```json
{
  "title": "技术平台升级事项",
  "topicType": "重大项目",
  "summary": "会议讨论过程及决议纪要",
  "conclusion": "同意按程序推进"
}
```

仅会议管理员可调用，响应 `data` 为更新后的议题对象。会议控制台只读展示 `conclusion`。

## 9. 看板接口

### 9.1 会议 dashboard

```http
GET {{baseUrl}}/meetings/{{meetingId}}/dashboard
Authorization: Bearer {{token}}
```

响应字段：

| 字段 | 说明 |
| --- | --- |
| currentTopic | 当前 `RUNNING` 议题，没有则为 null |
| signedCount | 已签到参会记录数 |
| attendeeCount | 议题参会记录总数 |
| topicCount | 议题总数 |
| signedTopicCount | 已签到议题数，按汇报部门签到统计 |
| topicSignStats | 每个议题的汇报部门签到状态 |

### 9.2 签到看板

```http
GET {{baseUrl}}/meetings/{{meetingId}}/signin-dashboard
Authorization: Bearer {{token}}
```

当前实现复用 dashboard 数据，前端在此页面额外调用 `sign-qrcode` 生成二维码。

## 10. 当前数据库表结构说明

### 10.1 t_wxdept

企业微信部门表。系统通过该表展示部门、按部门筛选通讯录、匹配议题汇报部门和参会部门。

| 字段 | 说明 |
| --- | --- |
| f_dept_id | 企业微信部门 ID，主键 |
| f_dept_nm | 部门名称 |
| f_order_index | 排序 |
| f_parent_dept_id | 上级部门 ID |
| f_create_time | 创建时间 |
| f_pathids | 部门路径 ID |
| f_pathnames | 部门路径名称 |
| f_tel | 部门电话 |
| f_deptnm_alias | 部门别名 |

### 10.2 t_wxdept_users

企业微信用户表。`f_user_id` 同时作为登录 ID、员工编码、业务用户 ID。

| 字段 | 说明 |
| --- | --- |
| f_user_id | 用户 ID，主键 |
| f_user_name | 用户姓名 |
| f_dept_id | 所属部门 ID |
| f_phone | 手机号 |
| f_create_time | 创建时间 |
| f_isleader | 企业微信部门负责人标记 |
| f_position | 职务 |
| f_email | 邮箱 |
| f_gender | 性别 |
| f_avatar | 头像 |
| f_english_name | 英文名 |
| f_status | 企业微信用户状态 |
| f_qr_code | 企业微信二维码 |
| f_telephone | 座机 |
| f_enable | 是否启用，`0` 表示停用 |

其他 `f_isQianWei`、`f_center_id`、`f_xndept_id`、`f_desc`、`f_emp_typ`、`f_emp_status`、`f_tb`、`f_tags`、`needauth_*` 为企微同步预留字段。

### 10.3 t_meeting_authority

会议角色权限表。系统登录和接口权限判断均按该表读取角色。

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| user_id | 用户 ID，关联 `t_wxdept_users.f_user_id` |
| user_name | 用户姓名快照 |
| dept_id | 权限所属部门 |
| authority | `1=ADMIN`，`2=LEADER`，`3=SECRETARY` |
| group_code | 权限分组，默认 `DEFAULT` |
| create_time | 创建时间 |

### 10.4 t_meeting_main

会议主表。

| 字段 | 说明 |
| --- | --- |
| id | 会议 ID |
| meeting_date | 会议日期 |
| meeting_time | 会议时间 |
| period_no | 会议期数 |
| location | 会议地点 |
| leaders | 参会领导展示文本 |
| content | 会议内容 |
| status | 会议状态 |
| creater | 创建人用户 ID |
| created_time | 创建时间 |
| updated_time | 更新时间 |

### 10.5 t_meeting_topic

会议议题表。

| 字段 | 说明 |
| --- | --- |
| id | 议题 ID |
| meeting_id | 会议 ID |
| topic_type | 议题类型 |
| title | 议题标题 |
| report_dept_id | 汇报部门 ID |
| report_dept_name | 汇报部门名称快照 |
| participant_dept_id | 参会部门 ID，逗号分隔 |
| participant_dept_name | 参会部门名称，逗号分隔 |
| summary | 会议纪要 |
| sort_no | 议题排序 |
| status | 议题状态 |
| start_time | 开始时间 |
| end_time | 结束时间 |
| actual_minutes | 实际时长，分钟 |
| conclusion | 会议结论 |
| report_dept_signed | 汇报部门是否已签到 |
| created_at | 创建时间 |
| updated_at | 更新时间 |

### 10.6 t_meeting_attendee

参会人员和签到统一表。

| 字段 | 说明 |
| --- | --- |
| id | 主键 |
| meeting_id | 会议 ID |
| topic_id | 议题 ID |
| user_id | 参会人用户 ID |
| user_name | 参会人姓名快照 |
| attendee_type | `REPORT/PARTAKE` |
| selected_by | 选择人员的用户 ID |
| selected_source | 选择来源，`ADMIN/DEPARTMENT` |
| selected_dept_id | 部门来源记录的所属部门 ID |
| created_at | 创建时间 |
| confirm_status | `PENDING/CONFIRMED` |
| confirmed_by | 确认人用户 ID |
| confirmed_at | 确认时间 |
| sign_status | `UNSIGNED/SIGNED` |
| signed_at | 签到时间 |

### 10.7 t_meeting_notification

通知落库表。当前企业微信和短信不真实发送，默认发送器只记录日志。

| 字段 | 说明 |
| --- | --- |
| id | 通知 ID |
| meeting_id | 会议 ID |
| topic_id | 议题 ID |
| title | 通知标题 |
| msg_body | 通知内容 |
| tousers | 接收用户 ID |
| send_time | 发送时间 |
| job_id | 发送任务 ID 或模拟任务 ID |
| created_at | 创建时间 |

### 10.8 t_meeting_uploaded_file

议题 Word 导入记录表。

| 字段 | 说明 |
| --- | --- |
| id | 上传记录 ID |
| meeting_id | 会议 ID |
| file_name | 文件名 |
| file_size | 文件大小，字节 |
| parser_status | `SUCCESS/FAILED` |
| parser_message | 解析结果说明 |
| created_at | 创建时间 |

## 11. Postman 推荐调试流程

1. `POST /auth/login` 使用 `U0001` 登录，保存 `token`。
2. `POST /meetings` 创建会议和议题。
3. `POST /meetings/{id}/publish` 发布会议。
4. 用 `U0002` 登录，调用 `GET /meetings/{id}/selection-tasks` 查看秘书任务。
5. 用 `U0002` 调用 `POST /topics/{topicId}/attendees` 选择 `U0004` 为 `REPORT`。
6. 用 `U0002` 调用 `POST /meetings/{id}/attendees/confirm` 确认科组长。
7. 用 `U0004` 登录，选择 `U0006` 为 `PARTAKE`。
8. 用 `U0004` 调用确认接口。
9. 用 `U0001` 调用 `POST /meetings/{id}/sign-qrcode` 生成签到 token。
10. 用 `U0006` 登录，调用 `GET /sign-in/preview?token=...` 查看本人议题。
11. 用 `U0006` 调用 `POST /sign-in` 签到。
12. 用 `U0001` 调用开始议题、结束议题、更新议题信息、dashboard。
