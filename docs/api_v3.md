# 会议管理系统 API 文档 v3

本文档根据当前后端 Controller、Service、Mapper XML 重新整理，适用于本地联调、Postman 调试和前端对接。

## 1. 基础约定

- 本地后端默认地址：`http://localhost:9090`
- API Base URL：`http://localhost:9090/api`
- 除 `POST /api/auth/login` 外，其它接口都需要登录 token。
- 统一响应结构：

```json
{
  "success": true,
  "message": "成功",
  "data": {}
}
```

业务异常返回 HTTP 400，系统异常返回 HTTP 500：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

认证请求头：

```http
Authorization: Bearer {{token}}
Content-Type: application/json
```

`Authorization` token 当前保存在后端内存中，服务重启后失效。

## 2. 角色与状态

### 2.1 用户角色

角色由 `t_meeting_authority.authority` 计算得到；没有权限记录的用户视为普通参会人。

| authority | role | 说明 |
| --- | --- | --- |
| 1 | ADMIN | 会议管理员，可创建、编辑、发布会议，可查看全部会议 |
| 2 | LEADER | 科组长，可处理被确认或分享给自己的相关议题 |
| 3 | SECRETARY | 部门秘书，可处理本部门作为汇报部门或参会部门的议题 |
| 无记录 | PARTICIPANT | 普通参会人，被确认参会后可查看会议并签到 |

### 2.2 会议状态

| 状态 | 说明 |
| --- | --- |
| DRAFT | 草稿，仅管理员可见 |
| PUBLISHED | 已发布，可开始参会人员选择、确认和通知 |
| IN_PROGRESS | 会议进行中 |
| FINISHED | 会议已结束 |

### 2.3 议题状态

| 状态 | 说明 |
| --- | --- |
| WAITING | 待开始 |
| RUNNING | 进行中 |
| FINISHED | 已结束 |

### 2.4 参会与签到状态

| 字段 | 状态 | 说明 |
| --- | --- | --- |
| `confirmStatus` | PENDING | 已被选择，但尚未确认；不会出现在本人会议列表中 |
| `confirmStatus` | CONFIRMED | 已确认参会；可出现在本人会议列表中并可签到 |
| `signStatus` | UNSIGNED | 未签到 |
| `signStatus` | SIGNED | 已签到 |

参会类型：

| attendeeType | 说明 |
| --- | --- |
| REPORT | 汇报人 |
| PARTAKE | 参会人 |

## 3. 认证接口

### 3.1 登录

```http
POST /api/auth/login
Content-Type: application/json
```

请求体支持按用户ID或姓名登录：

```json
{
  "userId": "U0001"
}
```

```json
{
  "username": "系统管理员"
}
```

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "token": "本地内存token",
    "user": {
      "id": "U0001",
      "userId": "U0001",
      "employeeNo": "U0001",
      "username": "系统管理员",
      "realName": "系统管理员",
      "departmentId": "D001",
      "departmentName": "办公室",
      "mobile": "13800000001",
      "email": "admin@example.com",
      "position": "会议管理员",
      "authorityValue": 1,
      "role": "ADMIN"
    }
  }
}
```

### 3.2 当前登录人

```http
GET /api/auth/me
Authorization: Bearer {{token}}
```

响应 `data` 为当前用户对象，字段同登录响应中的 `user`。

## 4. 通讯录接口

### 4.1 部门列表

```http
GET /api/departments
Authorization: Bearer {{token}}
```

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": [
    {
      "id": "D002",
      "deptId": "D002",
      "name": "规划部",
      "parentId": null,
      "sortNo": 2
    }
  ]
}
```

### 4.2 用户列表

```http
GET /api/users?departmentId=D002&role=LEADER&keyword=规划
Authorization: Bearer {{token}}
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| departmentId | 否 | 按部门ID过滤 |
| role | 否 | `ADMIN`、`LEADER`、`SECRETARY`、`PARTICIPANT` |
| keyword | 否 | 按用户ID或姓名模糊搜索 |

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": [
    {
      "id": "U0004",
      "userId": "U0004",
      "employeeNo": "U0004",
      "username": "规划部科组长",
      "realName": "规划部科组长",
      "departmentId": "D002",
      "departmentName": "规划部",
      "mobile": "13800000004",
      "role": "LEADER"
    }
  ]
}
```

## 5. 会议接口

### 5.1 会议列表

```http
GET /api/meetings
Authorization: Bearer {{token}}
```

可见性规则：

- `ADMIN`：查看全部会议，包括草稿。
- `SECRETARY`：查看已发布且本部门是汇报部门或参会部门的会议。
- `LEADER`、`PARTICIPANT`：查看本人已确认参会的会议。
- 拥有议题分享通知的人也可看到相关会议。
- 仅被选择但仍为 `PENDING` 的人员，不会看到该会议。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "meetingDate": "2026-07-22",
      "meetingTime": "15:00",
      "periodNo": "2026年第4期",
      "location": "第一会议室",
      "leaders": "分管领导",
      "content": "三重一大事项审议",
      "status": "PUBLISHED",
      "createdBy": "U0001",
      "topicCount": 2,
      "createdAt": "2026-07-22T10:00:00",
      "updatedAt": "2026-07-22T10:00:00"
    }
  ]
}
```

### 5.2 创建会议

```http
POST /api/meetings
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

仅 `ADMIN` 可调用。

请求体：

```json
{
  "meetingDate": "2026-07-22",
  "meetingTime": "15:00",
  "periodNo": "2026年第4期",
  "location": "第一会议室",
  "leaders": "分管领导",
  "content": "三重一大事项审议",
  "topics": [
    {
      "topicType": "三重一大",
      "title": "技术平台升级事项",
      "reportDepartmentId": "D002",
      "participantDepartmentIds": ["D002", "D003"],
      "summary": "讨论技术平台升级方案",
      "conclusion": "",
      "notice": "",
      "projectCode": "XM-2026-001",
      "sortNo": 1
    }
  ]
}
```

说明：

- `topics` 可为空，但空议题会议不能发布。
- `reportDepartmentName` 可不传，后端按部门ID补充。
- `participantDepartmentIds` 会保存为 `participantDeptId` 逗号分隔字符串。
- `participantDepartments` 可不传，后端按部门ID补充名称。
- 新会议状态固定为 `DRAFT`，新议题状态固定为 `WAITING`。

响应 `data` 为会议详情。

### 5.3 编辑会议

```http
PUT /api/meetings/{meetingId}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

仅 `ADMIN` 可调用。请求体同创建会议。

议题处理规则：

- `topics[].id` 存在：更新原议题。
- `topics[].id` 不存在：新增议题。
- 数据库中存在但本次请求未提交的议题：删除。
- 议题最终顺序以请求数组顺序和 `sortNo` 为准。

响应 `data` 为会议详情。

### 5.4 会议详情

```http
GET /api/meetings/{meetingId}
Authorization: Bearer {{token}}
```

兼容路径：

```http
GET /api/meetings/{meetingId}/detail
Authorization: Bearer {{token}}
```

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "id": 1,
    "meetingDate": "2026-07-22",
    "meetingTime": "15:00",
    "periodNo": "2026年第4期",
    "location": "第一会议室",
    "leaders": "分管领导",
    "content": "三重一大事项审议",
    "status": "PUBLISHED",
    "topics": [
      {
        "id": 10,
        "meetingId": 1,
        "topicType": "三重一大",
        "title": "技术平台升级事项",
        "reportDepartmentId": "D002",
        "reportDepartmentName": "规划部",
        "participantDeptId": "D002,D003",
        "participantDepartmentIds": ["D002", "D003"],
        "participantDepartments": "规划部,数据中心",
        "summary": "讨论技术平台升级方案",
        "notice": "",
        "projectCode": "XM-2026-001",
        "sortNo": 1,
        "status": "WAITING",
        "startTime": null,
        "endTime": null,
        "actualMinutes": null,
        "conclusion": "",
        "reportDepartmentSigned": 0,
        "canSelectReporter": true,
        "canSelectParticipant": true,
        "canShareTopic": false,
        "sharedAccess": false,
        "attendees": []
      }
    ],
    "attendees": [],
    "signIns": [],
    "topicSignStats": [
      {
        "id": 10,
        "sortNo": 1,
        "title": "技术平台升级事项",
        "reportDepartmentName": "规划部",
        "signed": false
      }
    ]
  }
}
```

权限补充：

- 非管理员不能查看 `DRAFT` 会议。
- 详情中的 `canSelectReporter`、`canSelectParticipant`、`canShareTopic`、`sharedAccess` 按当前用户角色和议题部门计算。

### 5.5 发布会议

```http
POST /api/meetings/{meetingId}/publish
Authorization: Bearer {{adminToken}}
```

仅 `ADMIN` 可调用。

发布规则：

- 会议至少需要一个议题。
- 会议状态改为 `PUBLISHED`。
- 后端按议题汇报部门和参会部门查找部门秘书，写入通知记录。
- 未找到秘书的部门会通知管理员。

响应 `data` 为会议详情。

### 5.6 导入议题到会议

```http
POST /api/meetings/{meetingId}/topics/import
Authorization: Bearer {{adminToken}}
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| file | File | Word 议题文件，仅支持 `.docx`，最大50MB |

行为：

- 解析 Word 文件。
- 删除该会议原有议题。
- 将解析出的议题写入 `t_meeting_topic`。
- 写入 `t_meeting_uploaded_file` 导入记录。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meetingId": 1,
    "fileName": "topics.docx",
    "parserStatus": "SUCCESS",
    "parserMessage": "识别到2个议题",
    "topicCount": 2,
    "topics": []
  }
}
```

### 5.7 只解析议题文件

```http
POST /api/meetings/topics/parse
Authorization: Bearer {{token}}
Content-Type: multipart/form-data
```

表单字段同导入接口。该接口只返回解析结果，不写入会议议题表。

## 6. 议题与参会人员接口

### 6.1 当前用户可处理议题

```http
GET /api/meetings/{meetingId}/selection-tasks
Authorization: Bearer {{token}}
```

规则：

- `SECRETARY`：返回本部门作为汇报部门或参会部门的议题。
- `LEADER`：返回本人已确认且类型为 `LEADER` 的议题。
- 有分享处理权的用户：返回被分享的议题。
- 其它情况：返回会议全部议题，但后续提交仍会按权限校验。

响应 `data` 为议题数组。

### 6.2 提交议题参会人员

```http
POST /api/topics/{topicId}/attendees
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

字段说明：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| attendeeType | 是 | `REPORT` 或 `PARTAKE`；兼容传入 `PARTICIPANT`，后端转为 `PARTAKE` |
| userIds | 是 | 用户ID数组，不能为空 |

规则：

- `ADMIN` 可选择任意议题人员，新增记录来源为 `ADMIN`。
- `SECRETARY` 可选择本部门对应类型的人员：汇报部门选择 `REPORT`，参会部门选择 `PARTAKE`。
- 有议题分享处理权的用户可协助选择本部门人员。
- 被选择人员必须属于该议题对应部门。
- 单议题参会记录数量不能超过 `meeting.max-attendees-per-topic`，默认60。
- 新增记录初始 `confirmStatus` 为 `PENDING`，需要后续确认后才可签到。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "topic": {},
    "attendees": [
      {
        "id": 1,
        "meeting_id": 1,
        "topic_id": 10,
        "user_id": "U0004",
        "user_name": "规划部科组长",
        "attendee_type": "REPORT",
        "selected_by": "U0002",
        "selected_source": "DEPARTMENT",
        "selected_dept_id": "D002",
        "confirm_status": "PENDING",
        "sign_status": "UNSIGNED",
        "department_id": "D002",
        "department_name": "规划部"
      }
    ]
  }
}
```

### 6.3 更新议题基础信息

```http
PUT /api/topics/{topicId}
Authorization: Bearer {{adminToken}}
Content-Type: application/json
```

仅 `ADMIN` 可调用。

请求体：

```json
{
  "title": "技术平台升级事项",
  "topicType": "重大项目",
  "summary": "会议讨论过程及纪要",
  "conclusion": "同意按程序推进"
}
```

该接口只更新 `title`、`topicType`、`summary`、`conclusion`。

### 6.4 通知入场准备或分享议题处理权

```http
POST /api/topics/{topicId}/notify-preparation
Authorization: Bearer {{token}}
Content-Type: application/json
```

发送入场准备通知：

```json
{
  "attendeeUserIds": ["U0004", "U0006"]
}
```

分享议题处理权：

```json
{
  "shareUserIds": ["U0006"]
}
```

规则：

- 会议必须不是 `DRAFT`。
- `ADMIN`、`SECRETARY`、`LEADER` 或已有分享处理权的人可调用，但还会校验是否可操作当前议题。
- 传 `shareUserIds` 时优先执行分享逻辑。
- 分享对象必须是当前用户同部门人员。
- 不传 `attendeeUserIds` 时，通知当前议题下全部 `PENDING` 参会记录。

普通通知响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meetingId": 1,
    "topicId": 10,
    "notifiedCount": 2,
    "attendees": []
  }
}
```

分享响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meetingId": 1,
    "topicId": 10,
    "sharedCount": 1,
    "attendees": []
  }
}
```

### 6.5 确认参会人员

```http
POST /api/meetings/{meetingId}/attendees/confirm
Authorization: Bearer {{token}}
```

规则：

- `SECRETARY` 和 `LEADER` 可调用。
- 会议不能是 `DRAFT`。
- 后端确认当前登录人自己选择且仍为 `PENDING` 的参会记录。
- 确认后写入 `confirmed_by`、`confirmed_at`，并写入通知记录。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meetingId": 1,
    "confirmedCount": 2,
    "attendees": []
  }
}
```

## 7. 签到接口

### 7.1 生成签到 token

```http
POST /api/meetings/{meetingId}/sign-qrcode
Authorization: Bearer {{token}}
```

当前 Controller 未限制角色，调用方需要在前端入口控制。token 存在后端内存，默认有效期由 `meeting.sign-token-ttl-minutes` 控制，默认240分钟。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meetingId": 1,
    "token": "b6b7d8e7...",
    "expiresAt": "2026-07-22T18:00:00",
    "url": "/mobile/sign-in?token=b6b7d8e7..."
  }
}
```

### 7.2 签到预览

```http
GET /api/sign-in/preview?token={{signToken}}
Authorization: Bearer {{token}}
```

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "meeting": {
      "id": 1,
      "meetingDate": "2026-07-22",
      "meetingTime": "15:00",
      "location": "第一会议室",
      "status": "PUBLISHED"
    },
    "topics": [
      {
        "attendeeId": 10,
        "meetingId": 1,
        "topicId": 10,
        "userId": "U0006",
        "userName": "规划部参会人",
        "attendeeType": "PARTAKE",
        "signStatus": "UNSIGNED",
        "signed": false,
        "title": "技术平台升级事项",
        "sortNo": 1
      }
    ],
    "canSign": true,
    "message": "",
    "expiresAt": "2026-07-22T18:00:00"
  }
}
```

### 7.3 确认签到

```http
POST /api/sign-in
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
- 一次签到会将该用户在该会议下所有已确认且未签到的参会记录改为 `SIGNED`。
- 如果当前用户部门是议题汇报部门，会同步更新该议题 `report_dept_signed=1`。
- 重复签到会返回业务异常。

响应：

```json
{
  "success": true,
  "message": "成功",
  "data": {
    "message": "签到成功",
    "topics": []
  }
}
```

## 8. 会议控制台接口

### 8.1 开始议题

```http
POST /api/topics/{topicId}/start
Authorization: Bearer {{token}}
```

行为：

- 当前会议状态改为 `IN_PROGRESS`。
- 当前议题状态改为 `RUNNING`。
- 写入 `startTime`。
- 同一会议已有 `RUNNING` 议题时，不允许开始新议题。

响应 `data` 为 dashboard 聚合数据。

### 8.2 结束议题

```http
POST /api/topics/{topicId}/end
Authorization: Bearer {{token}}
Content-Type: application/json
```

请求体可为空，也可手动指定耗时：

```json
{
  "actualMinutes": 12
}
```

行为：

- 当前议题必须是 `RUNNING`。
- 写入 `endTime`。
- `actualMinutes` 优先使用请求值；不传时按 `startTime` 到当前时间计算，最少1分钟。
- 如果会议下所有议题都为 `FINISHED`，会议状态改为 `FINISHED`。

响应 `data` 为 dashboard 聚合数据。

### 8.3 会议看板

```http
GET /api/meetings/{meetingId}/dashboard
Authorization: Bearer {{token}}
```

响应 `data` 是会议详情并追加以下字段：

| 字段 | 说明 |
| --- | --- |
| currentTopic | 当前 `RUNNING` 议题，没有则为 `null` |
| signedCount | 已签到参会记录数量 |
| attendeeCount | 参会记录总数 |
| topicCount | 议题总数 |
| signedTopicCount | 已完成汇报部门签到的议题数量 |
| topicSignStats | 各议题汇报部门签到状态 |

### 8.4 签到看板

```http
GET /api/meetings/{meetingId}/signin-dashboard
Authorization: Bearer {{token}}
```

当前实现复用 dashboard 数据，移动端签到详情页可以直接使用 `topicSignStats` 展示各议题签到情况，不需要展示签到码。

## 9. 数据模型字段说明

### 9.1 会议对象

| 字段 | 说明 |
| --- | --- |
| id | 会议ID |
| meetingDate | 会议日期 |
| meetingTime | 会议时间 |
| periodNo | 会议期次 |
| location | 会议地点 |
| leaders | 参会或主持领导 |
| content | 会议内容摘要 |
| status | 会议状态 |
| createdBy | 创建人用户ID |
| createdAt | 创建时间 |
| updatedAt | 更新时间 |
| topicCount | 议题数量 |

### 9.2 议题对象

| 字段 | 说明 |
| --- | --- |
| id | 议题ID |
| meetingId | 会议ID |
| topicType | 议题类型 |
| title | 议题标题 |
| reportDepartmentId | 汇报部门ID，多个逗号分隔 |
| reportDepartmentName | 汇报部门名称 |
| participantDeptId | 参会部门ID，多个逗号分隔 |
| participantDepartmentIds | 参会部门ID数组，由后端从 `participantDeptId` 拆分 |
| participantDepartments | 参会部门名称 |
| summary | 议题摘要或会议纪要 |
| notice | Word解析出的通知或说明 |
| projectCode | Word解析出的项目编号 |
| sortNo | 排序号 |
| status | 议题状态 |
| startTime | 开始时间 |
| endTime | 结束时间 |
| actualMinutes | 实际耗时，分钟 |
| conclusion | 议题结论 |
| reportDepartmentSigned | 汇报部门是否签到 |
| attendees | 当前议题参会记录 |

### 9.3 参会记录对象

| 字段 | 说明 |
| --- | --- |
| id | 参会记录ID |
| meetingId / meeting_id | 会议ID |
| topicId / topic_id | 议题ID |
| userId / user_id | 参会人用户ID |
| userName / user_name | 参会人姓名 |
| attendeeType / attendee_type | `REPORT` 或 `PARTAKE` |
| selectedBy / selected_by | 选择人用户ID |
| selectedSource / selected_source | `ADMIN` 或 `DEPARTMENT` |
| selectedDeptId / selected_dept_id | 选择来源部门ID |
| confirmStatus / confirm_status | `PENDING` 或 `CONFIRMED` |
| confirmedBy / confirmed_by | 确认人用户ID |
| confirmedAt / confirmed_at | 确认时间 |
| signStatus / sign_status | `UNSIGNED` 或 `SIGNED` |
| signedAt / signed_at | 签到时间 |
| departmentId / department_id | 用户所属部门ID |
| departmentName / department_name | 用户所属部门名称 |

## 10. 推荐联调流程

1. 使用 `POST /api/auth/login` 以 `U0001` 登录管理员。
2. 调用 `POST /api/meetings` 创建会议和议题。
3. 调用 `POST /api/meetings/{meetingId}/publish` 发布会议。
4. 使用部门秘书账号登录，调用 `GET /api/meetings/{meetingId}/selection-tasks` 查看可处理议题。
5. 调用 `POST /api/topics/{topicId}/attendees` 选择汇报人或参会人。
6. 调用 `POST /api/meetings/{meetingId}/attendees/confirm` 确认参会人员。
7. 已确认人员登录后调用 `GET /api/meetings` 查看自己的会议。
8. 调用 `POST /api/meetings/{meetingId}/sign-qrcode` 生成签到 token。
9. 已确认人员调用 `GET /api/sign-in/preview?token=...` 查看签到议题。
10. 调用 `POST /api/sign-in` 完成签到。
11. 控制台调用 `POST /api/topics/{topicId}/start` 和 `POST /api/topics/{topicId}/end` 推进议题。
12. 调用 `GET /api/meetings/{meetingId}/dashboard` 或 `/signin-dashboard` 查看控制台和签到统计。
